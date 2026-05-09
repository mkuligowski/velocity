package com.mkuligowski.velocity.loads.adapters.db.sql;

public final class LoadAttemptSql {

    public static final String INSERT_EVENT = """
            INSERT INTO load_attempt_event
                (event_id, customer_id, load_id, event_type, event_payload, occurred_at, recorded_at)
            VALUES
                (:event_id, :customer_id, :load_id, :event_type, :event_payload, :occurred_at, :recorded_at)
            """;

    public static final String INSERT_SNAPSHOT = """
            INSERT INTO load_attempt_snapshot
                (customer_id, load_id, amount, load_time, accepted)
            VALUES
                (:customer_id, :load_id, :amount, :load_time, :accepted)
            """;

    private LoadAttemptSql() {}
}
