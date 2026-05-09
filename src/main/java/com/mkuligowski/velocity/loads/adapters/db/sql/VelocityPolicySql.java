package com.mkuligowski.velocity.loads.adapters.db.sql;

public final class VelocityPolicySql {

    public static final String FIND_ALL = """
            SELECT policy_id,
                   policy_type,
                   time_window,
                   threshold_amount,
                   threshold_count,
                   decline_reason
              FROM velocity_policy
            """;

    private VelocityPolicySql() {}
}
