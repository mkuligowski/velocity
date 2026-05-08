package com.mkuligowski.velocity.loads.domain.model;

import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record LoadAttemptDeclined(
        LoadId loadId,
        CustomerId customerId,
        Money amount,
        Instant loadTime,
        List<LoadAttemptEvent> events,
        DeclineReason declineReason
) implements LoadAttempt {

    public LoadAttemptDeclined {
        Objects.requireNonNull(loadId, "loadId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(loadTime, "loadTime must not be null");
        Objects.requireNonNull(events, "events must not be null");
        Objects.requireNonNull(declineReason, "declineReason must not be null");
        events = List.copyOf(events);
    }
}
