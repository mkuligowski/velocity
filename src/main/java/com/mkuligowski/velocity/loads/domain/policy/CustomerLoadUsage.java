package com.mkuligowski.velocity.loads.domain.policy;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.Objects;

public record CustomerLoadUsage(
        Money dailySum,
        int dailyCount,
        Money weeklySum,
        int weeklyCount
) {

    public CustomerLoadUsage {
        Objects.requireNonNull(dailySum, "dailySum must not be null");
        Objects.requireNonNull(weeklySum, "weeklySum must not be null");
        if (dailyCount < 0) {
            throw new IllegalArgumentException("dailyCount must be non-negative: " + dailyCount);
        }
        if (weeklyCount < 0) {
            throw new IllegalArgumentException("weeklyCount must be non-negative: " + weeklyCount);
        }
    }

    public static CustomerLoadUsage empty() {
        return new CustomerLoadUsage(Money.ZERO, 0, Money.ZERO, 0);
    }
}
