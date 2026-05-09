package com.mkuligowski.velocity.loads.adapters.db.sql;

public final class CustomerLoadUsageSql {

    public static final String SUM_AND_COUNT_IN_WINDOW = """
            SELECT COALESCE(SUM(amount), 0) AS total_amount,
                   COUNT(*)                  AS total_count
              FROM load_attempt_snapshot
             WHERE customer_id = :customer_id
               AND accepted    = TRUE
               AND load_time  >= :window_start
               AND load_time  <  :window_end
            """;

    private CustomerLoadUsageSql() {}
}
