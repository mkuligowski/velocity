package com.mkuligowski.velocity.loads.adapters.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptAcceptedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptDeclinedEvent;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Flattened JSON shape persisted in {@code load_attempt_event.event_payload}. Independent
 * from the domain event records so the wire format is stable across domain refactors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoadAttemptEventPayload(
        UUID eventId,
        String loadId,
        String customerId,
        BigDecimal amount,
        Instant loadTime,
        Instant occurredAt,
        @Nullable DeclineReason declineReason
) {

    public static LoadAttemptEventPayload from(LoadAttemptEvent event) {
        return switch (event) {
            case LoadAttemptAcceptedEvent e -> new LoadAttemptEventPayload(
                    e.eventId(),
                    e.loadId().value(),
                    e.customerId().value(),
                    e.amount().value(),
                    e.loadTime(),
                    e.occurredAt(),
                    null);
            case LoadAttemptDeclinedEvent e -> new LoadAttemptEventPayload(
                    e.eventId(),
                    e.loadId().value(),
                    e.customerId().value(),
                    e.amount().value(),
                    e.loadTime(),
                    e.occurredAt(),
                    e.declineReason());
        };
    }
}
