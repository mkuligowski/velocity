package com.mkuligowski.velocity.loads.adapters.db.sql;

public final class CustomerLockSql {

    /** H2 upsert — inserts the row if missing, no-op if already present. */
    public static final String UPSERT = """
            MERGE INTO customer_lock (customer_id, created_at)
            KEY (customer_id)
            VALUES (:customer_id, :created_at)
            """;

    /** Acquires an exclusive row lock held until the surrounding transaction ends. */
    public static final String SELECT_FOR_UPDATE = """
            SELECT customer_id FROM customer_lock WHERE customer_id = :customer_id FOR UPDATE
            """;

    private CustomerLockSql() {}
}
