package com.mkuligowski.velocity.loads.domain.model;

import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptAcceptedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptDeclinedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public sealed interface LoadAttempt
        permits LoadAttemptInitiated, LoadAttemptAccepted, LoadAttemptDeclined {

    LoadId loadId();

    CustomerId customerId();

    Money amount();

    Instant loadTime();

    List<LoadAttemptEvent> events();

    static LoadAttemptInitiated initiate(LoadId loadId, CustomerId customerId, Money amount, Instant loadTime) {
        return new LoadAttemptInitiated(loadId, customerId, amount, loadTime);
    }

    static LoadAttempt fromEvents(List<LoadAttemptEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Cannot rehydrate LoadAttempt from empty event list");
        }
        List<LoadAttemptEvent> sorted = events.stream()
                .sorted(Comparator.comparing(LoadAttemptEvent::occurredAt))
                .toList();
        LoadAttemptEvent terminal = sorted.get(sorted.size() - 1);
        return switch (terminal) {
            case LoadAttemptAcceptedEvent e -> new LoadAttemptAccepted(
                    e.loadId(), e.customerId(), e.amount(), e.loadTime(), sorted);
            case LoadAttemptDeclinedEvent e -> new LoadAttemptDeclined(
                    e.loadId(), e.customerId(), e.amount(), e.loadTime(), sorted, e.declineReason());
        };
    }
}
