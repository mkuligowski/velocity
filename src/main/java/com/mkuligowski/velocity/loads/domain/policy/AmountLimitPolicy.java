package com.mkuligowski.velocity.loads.domain.policy;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.Objects;
import java.util.Optional;

public record AmountLimitPolicy(
        PolicyId id,
        TimeWindow window,
        Money threshold,
        DeclineReason declineReason
) implements VelocityPolicy {

    public AmountLimitPolicy {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(threshold, "threshold must not be null");
        Objects.requireNonNull(declineReason, "declineReason must not be null");
    }

    @Override
    public Optional<DeclineReason> check(CustomerLoadUsage usage, Money candidate) {
        Money sum = window == TimeWindow.DAY ? usage.dailySum() : usage.weeklySum();
        return sum.add(candidate).compareTo(threshold) > 0
                ? Optional.of(declineReason)
                : Optional.empty();
    }
}
