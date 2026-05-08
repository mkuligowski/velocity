package com.mkuligowski.velocity.loads.domain.model.events;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import com.mkuligowski.velocity.loads.domain.model.Money;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LoadAttemptDeclinedEvent(
        UUID eventId,
        LoadId loadId,
        CustomerId customerId,
        Money amount,
        Instant loadTime,
        Instant occurredAt,
        DeclineReason declineReason
) implements LoadAttemptEvent {

    public LoadAttemptDeclinedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(loadId, "loadId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(loadTime, "loadTime must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(declineReason, "declineReason must not be null");
    }

    @Override
    public Type type() {
        return Type.DECLINED;
    }
}
