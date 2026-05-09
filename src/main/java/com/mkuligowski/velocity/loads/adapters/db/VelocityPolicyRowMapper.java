package com.mkuligowski.velocity.loads.adapters.db;

import com.mkuligowski.velocity.loads.domain.model.Money;
import com.mkuligowski.velocity.loads.domain.policy.AmountLimitPolicy;
import com.mkuligowski.velocity.loads.domain.policy.CountLimitPolicy;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import com.mkuligowski.velocity.loads.domain.policy.PolicyId;
import com.mkuligowski.velocity.loads.domain.policy.TimeWindow;
import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class VelocityPolicyRowMapper implements RowMapper<VelocityPolicy> {

    static final VelocityPolicyRowMapper INSTANCE = new VelocityPolicyRowMapper();

    private VelocityPolicyRowMapper() {}

    @Override
    public VelocityPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
        var id = PolicyId.of(UUID.fromString(rs.getString("policy_id")));
        var window = TimeWindow.valueOf(rs.getString("time_window"));
        var reason = DeclineReason.valueOf(rs.getString("decline_reason"));
        var policyType = rs.getString("policy_type");

        // The CHECK constraint on velocity_policy guarantees the threshold column matches
        // the policy_type, so we can read directly without further defensive checks.
        return switch (policyType) {
            case "AMOUNT_LIMIT" -> new AmountLimitPolicy(
                    id, window, new Money(rs.getBigDecimal("threshold_amount")), reason);
            case "COUNT_LIMIT" -> new CountLimitPolicy(
                    id, window, rs.getInt("threshold_count"), reason);
            default -> throw new IllegalStateException(
                    "Unknown policy_type in velocity_policy row: " + policyType);
        };
    }
}
