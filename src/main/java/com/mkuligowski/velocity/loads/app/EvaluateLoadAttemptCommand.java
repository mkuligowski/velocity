package com.mkuligowski.velocity.loads.app;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import com.mkuligowski.velocity.loads.domain.model.Money;
import java.time.Instant;
import java.util.Objects;

public record EvaluateLoadAttemptCommand(
        LoadId loadId,
        CustomerId customerId,
        Money amount,
        Instant loadTime
) {

    public EvaluateLoadAttemptCommand {
        Objects.requireNonNull(loadId, "loadId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(loadTime, "loadTime must not be null");
    }
}
