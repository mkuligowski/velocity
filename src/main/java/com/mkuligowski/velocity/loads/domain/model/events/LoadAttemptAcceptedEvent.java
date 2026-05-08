package com.mkuligowski.velocity.loads.domain.model.events;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import com.mkuligowski.velocity.loads.domain.model.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LoadAttemptAcceptedEvent(
        UUID eventId,
        LoadId loadId,
        CustomerId customerId,
        Money amount,
        Instant loadTime,
        Instant occurredAt
) implements LoadAttemptEvent {

    public LoadAttemptAcceptedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(loadId, "loadId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(loadTime, "loadTime must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    @Override
    public Type type() {
        return Type.ACCEPTED;
    }
}
