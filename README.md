# Velocity

Small Spring Boot service that decides whether to accept or decline a fund-load attempt
based on per-customer velocity limits.

## What it does

`POST /api/v1/loads` takes a single load attempt as JSON:

```json
{ "id": "1234", "customer_id": "1234", "load_amount": "$123.45", "time": "2018-01-01T00:00:00Z" }
```

and returns:

- `200 OK` with `{ "id": "...", "customer_id": "...", "accepted": true|false }` for a new attempt
- `204 No Content` when the same `(customer_id, id)` was already processed (silent dedup, per spec)
- `400 Bad Request` for malformed input, with a small error envelope and a correlation id

## Limits

The spec defines three limits per customer:

- max `$5,000` loaded per UTC day
- max `$20,000` loaded per ISO week (Mon 00:00 → Sun 23:59:59 UTC)
- max `3` loads per UTC day

These three are inserted into the `velocity_policy` table at startup by Liquibase, but the
table is built so we can configure **any set of limits without changing code**. Two policy
types exist: amount-based and count-based, each can be applied to a daily or weekly window.
Adding another limit of an existing type is one SQL row. Adding a brand-new kind of rule
(e.g., per-load max amount) is one new permitted record in the sealed `VelocityPolicy`
hierarchy plus one row.

## How a request is processed

This is the part I want to be clear about, because it is where the actual safety
properties come from. Every `POST /api/v1/loads` goes through the same fixed sequence
inside one `@Transactional` method (`LoadAttemptEvaluationService.evaluate`). The order
matters — each step is there for a specific reason.

**1. Duplicate check — `idempotency.tryLock(customerId, loadId, now)`**

The very first thing we do is try to `INSERT` a row into `load_attempt_idempotency`. The
table has a primary key on `(customer_id, load_id)`. If this exact attempt was already
seen — same customer, same load id — the INSERT fails with a constraint violation, we
catch it, and the service returns `Optional.empty()`. The controller translates that into
a `204 No Content`. The duplicate is silently dropped, no further work is done, no
decision is recorded twice.

The gate is the database itself, not application memory, so this works the same with one
pod or twenty. Two pods racing on the same key get one winner and one loser — there is no
third outcome.

**2. Per-customer pessimistic lock — `customerLock.acquireFor(customerId)`**

Before reading anything about this customer's running totals, we take an exclusive
`SELECT … FOR UPDATE` lock on the customer's row in `customer_lock`. Any other request
for **the same customer**, on any pod, blocks here until our transaction commits.
Requests for **different customers** do not block each other.

Why this step exists and why it has to be **before** the read: if two threads first read
the daily total (`$4000`), then both decide "`$4000 + $1000` is still under `$5000`",
they both write and both commit, and now the customer is at `$6000`. Acquiring the lock
first means the second thread cannot do its read until the first one has committed — so
it sees `$5000`, not `$4000`.

**3. Load the active policy set — `policyRepository.findAll()`**

Selects every row from `velocity_policy`. The three spec rules are seeded by Liquibase,
but the table is the source of truth — a future fourth rule is a SQL `INSERT`, no code
change. The row mapper switches on `policy_type` and produces the right permitted record
(`AmountLimitPolicy` or `CountLimitPolicy`).

**4. Read this customer's running usage — `usageQuery.forCustomerOn(customerId, time)`**

Two aggregate queries against `load_attempt_snapshot`, filtered by `accepted = TRUE`:

- `SUM(amount)` + `COUNT(*)` for the UTC day window of `time`
- `SUM(amount)` + `COUNT(*)` for the ISO week window of `time`

Window math (Monday 00:00 UTC week start, etc.) lives in `UtcTimeBuckets`. Because step 2
is holding the customer's row lock, these reads are guaranteed to be **after** the commit
of any prior accepted load for this customer.

**5. Decide — `VelocityPolicyEvaluator.evaluate(policies, usage, candidate)`**

A pure function with no I/O. Walks the policy list, calls `policy.check(usage, candidate)`
on each, returns the first decline reason or `Accepted`. This is the single most testable
piece of the project — `VelocityPolicyEvaluatorTest` covers every branch.

**6. Persist the outcome — `repository.save(...)`**

Whether the decision is `Accepted` or `Declined`, two rows are written in the same
transaction:

- an event into `load_attempt_event` (audit log, full event payload as JSON)
- a snapshot into `load_attempt_snapshot` (the row that step 4 of the next request will
  read)

Declined attempts are recorded too. They do not count toward the limits, but they belong
in the audit trail.

**7. Commit — implicit at the `@Transactional` boundary**

The transaction commits. The `customer_lock` row is released. The new snapshot row
becomes visible to whichever pod processes the next request for this customer.

---

The properties of the service drop out of this sequence:

| Property | Why it holds |
|---|---|
| Same `(customer_id, id)` is never processed twice | Step 1 — DB unique constraint, race-safe across pods |
| Velocity limits hold under concurrent same-customer traffic | Step 2 — DB row lock acquired before step 4's read |
| Different customers do not serialize against each other | Step 2 lock is **per-customer**, not global |
| Every decision is auditable | Step 6 writes an immutable event row alongside the snapshot |

## How to run

Needs Java 25.

```bash
./gradlew bootRun           # starts on :8080
./gradlew check             # runs unit + integration + arch tests
./simulate.sh               # boots the app, replays input.txt, diffs against output.txt
```

`./simulate.sh` is the end-to-end check that the assignment expects: it starts the
service, fires every line of `input.txt` through the HTTP endpoint, collects each
response, and confirms the captured output matches `output.txt` byte-for-byte (modulo
line endings — `output.txt` is CRLF). Captured output goes to
`build/replay/actual-output.txt` so it can be inspected after the run. Exits 0 on a
match, 1 with the first diff hunks otherwise.

Quick smoke test against an already-running app:

```bash
curl -X POST localhost:8080/api/v1/loads \
  -H 'Content-Type: application/json' \
  -d '{"id":"1","customer_id":"42","load_amount":"$1000.00","time":"2018-01-01T00:00:00Z"}'
```

H2 console is at `http://localhost:8080/h2-console` (jdbc url `jdbc:h2:mem:velocity`,
user `sa`, no password).

## Storage

In-memory **H2**. Schema is managed by Liquibase (`src/main/resources/db/changelog/`).

**No ORM.** This was intentional — I'm not a fan of JPA / Hibernate. They hide too much,
the SQL you actually run is hard to see, and the moment performance matters you end up
fighting the framework. So: Spring's `NamedParameterJdbcTemplate` only. SQL is hand-written
and lives in `adapters/db/sql/`, one file per repository, so any query is one grep away.
I lose compile-time column-name safety compared to jOOQ, but for four tables and a handful
of queries that trade-off is fine. Integration tests run every query against the real
schema, so a typo blows up loudly there.

If this were Postgres or any other serious database I'd actually pick **jOOQ** — typed
SQL, schema-driven codegen, no leaky abstraction. I tried it here first and it worked, but
the codegen setup against H2 + Spring Boot 4 was fighting me on version alignment, so for
an in-memory take-home it was not worth the friction.

Five tables:

- `load_attempt_event` — append-only audit log, JSON payload per event
- `load_attempt_snapshot` — denormalized read model for `SUM` / `COUNT` aggregate queries
- `load_attempt_idempotency` — duplicate detection gate
- `customer_lock` — per-customer pessimistic lock row (see concurrency section)
- `velocity_policy` — the rule set (seeded with the three spec rules)

## Architecture

Hexagonal / ports-and-adapters. The package layout enforces it:

```
loads/
├── domain/          # pure Java — no Spring, no JDBC, no annotations from frameworks
│   ├── model/       # LoadAttempt sealed aggregate + events + value objects
│   └── policy/      # VelocityPolicy sealed type + permitted records + evaluator
├── app/             # application service + port interfaces
│   └── ports/       # repository / idempotency / policy lookup
└── adapters/
    ├── db/          # JdbcTemplate implementations + SQL constants
    ├── rest/        # @RestController + DTOs + error handler + correlation-id filter
    └── spring/      # Clock @Bean
```

The rules:

- `domain` depends on nothing project-specific
- `app` depends only on `domain`
- `adapters` depend on `app` (via ports) and `domain`
- adapters never depend on each other

ArchUnit verifies this, plus naming conventions (`@Service` ends in Service, etc.) and a
"no `Instant.now()` in production code" rule that forces injecting a `Clock` so time stays
deterministic in tests.

The aggregate (`LoadAttempt`) is a sealed interface with three permitted records:
`Initiated`, `Accepted`, `Declined`. State transitions are methods returning the next
state plus the event that caused it. This is lightweight event sourcing — every decision
writes one event row alongside the snapshot. It is over-engineered for three policies and
one transition, kept because it matches the team's house style and the audit trail comes
for free.

## Tech

Spring Boot 4.0.6, Java 25, Liquibase 5, H2, Jackson 3, JUnit 5, ArchUnit. Lombok and
MapStruct are wired in the build but barely used; records cover most of what they would
have done.

A note on Spring Boot 4: a lot of auto-config was split into per-tech modules
(`spring-boot-liquibase`, `spring-boot-jdbc`, `spring-boot-restclient`,
`spring-boot-resttestclient`, …). If you read `build.gradle.kts` and wonder why so many
explicit `spring-boot-*` dependencies, that's the reason.
