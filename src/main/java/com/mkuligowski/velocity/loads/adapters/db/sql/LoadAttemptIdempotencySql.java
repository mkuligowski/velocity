package com.mkuligowski.velocity.loads.adapters.db.sql;

public final class LoadAttemptIdempotencySql {

    public static final String INSERT = """
            INSERT INTO load_attempt_idempotency (customer_id, load_id, locked_at)
            VALUES (:customer_id, :load_id, :locked_at)
            """;

    private LoadAttemptIdempotencySql() {}
}
