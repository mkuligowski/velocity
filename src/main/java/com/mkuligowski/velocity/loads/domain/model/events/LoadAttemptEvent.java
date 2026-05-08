package com.mkuligowski.velocity.loads.domain.model.events;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import com.mkuligowski.velocity.loads.domain.model.Money;
import com.mkuligowski.velocity.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public sealed interface LoadAttemptEvent extends DomainEvent
        permits LoadAttemptAcceptedEvent, LoadAttemptDeclinedEvent {

    UUID eventId();

    LoadId loadId();

    CustomerId customerId();

    Money amount();

    Instant loadTime();

    Instant occurredAt();

    Type type();

    enum Type {
        ACCEPTED,
        DECLINED
    }
}
