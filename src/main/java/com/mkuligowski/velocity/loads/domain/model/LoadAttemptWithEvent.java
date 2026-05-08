package com.mkuligowski.velocity.loads.domain.model;

import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import java.util.Objects;

public record LoadAttemptWithEvent<S extends LoadAttempt, E extends LoadAttemptEvent>(
        S state,
        E event
) {

    public LoadAttemptWithEvent {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(event, "event must not be null");
    }
}
