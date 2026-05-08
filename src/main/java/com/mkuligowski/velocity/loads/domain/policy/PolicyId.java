package com.mkuligowski.velocity.loads.domain.policy;

import java.util.Objects;
import java.util.UUID;

public record PolicyId(UUID value) {

    public PolicyId {
        Objects.requireNonNull(value, "PolicyId value must not be null");
    }

    public static PolicyId of(UUID value) {
        return new PolicyId(value);
    }

    public static PolicyId of(String uuidValue) {
        return new PolicyId(UUID.fromString(uuidValue));
    }
}
