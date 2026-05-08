package com.mkuligowski.velocity.loads.domain.model;

import java.util.Objects;

public record LoadId(String value) {

    public LoadId {
        Objects.requireNonNull(value, "LoadId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("LoadId value must not be blank");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException("LoadId value must be <= 64 chars: " + value.length());
        }
    }

    public static LoadId of(String value) {
        return new LoadId(value);
    }
}
