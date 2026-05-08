package com.mkuligowski.velocity.loads.domain.policy;

import java.util.Objects;

public sealed interface Decision permits Decision.Accepted, Decision.Declined {

    record Accepted() implements Decision {
        public static final Accepted INSTANCE = new Accepted();
    }

    record Declined(DeclineReason reason) implements Decision {
        public Declined {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    static Decision accepted() {
        return Accepted.INSTANCE;
    }

    static Decision declined(DeclineReason reason) {
        return new Declined(reason);
    }
}
