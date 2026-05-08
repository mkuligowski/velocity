package com.mkuligowski.velocity.loads.domain.model;

import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptAcceptedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptDeclinedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LoadAttemptInitiated(
        LoadId loadId,
        CustomerId customerId,
        Money amount,
        Instant loadTime
) implements LoadAttempt {

    public LoadAttemptInitiated {
        Objects.requireNonNull(loadId, "loadId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(loadTime, "loadTime must not be null");
    }

    @Override
    public List<LoadAttemptEvent> events() {
        return List.of();
    }

    public LoadAttemptWithEvent<LoadAttemptAccepted, LoadAttemptAcceptedEvent> accept(
            UUID eventId, Instant occurredAt) {
        var event = new LoadAttemptAcceptedEvent(
                eventId, loadId, customerId, amount, loadTime, occurredAt);
        var nextState = new LoadAttemptAccepted(
                loadId, customerId, amount, loadTime, List.of(event));
        return new LoadAttemptWithEvent<>(nextState, event);
    }

    public LoadAttemptWithEvent<LoadAttemptDeclined, LoadAttemptDeclinedEvent> decline(
            DeclineReason reason, UUID eventId, Instant occurredAt) {
        var event = new LoadAttemptDeclinedEvent(
                eventId, loadId, customerId, amount, loadTime, occurredAt, reason);
        var nextState = new LoadAttemptDeclined(
                loadId, customerId, amount, loadTime, List.of(event), reason);
        return new LoadAttemptWithEvent<>(nextState, event);
    }
}
