package com.mkuligowski.velocity.loads.domain.policy;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.Objects;
import java.util.Optional;

public record CountLimitPolicy(
        PolicyId id,
        TimeWindow window,
        int threshold,
        DeclineReason declineReason
) implements VelocityPolicy {

    public CountLimitPolicy {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(declineReason, "declineReason must not be null");
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be non-negative: " + threshold);
        }
    }

    @Override
    public Optional<DeclineReason> check(CustomerLoadUsage usage, Money candidate) {
        int count = window == TimeWindow.DAY ? usage.dailyCount() : usage.weeklyCount();
        return count >= threshold
                ? Optional.of(declineReason)
                : Optional.empty();
    }
}
