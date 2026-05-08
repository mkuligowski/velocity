package com.mkuligowski.velocity.loads.domain.model;

import java.util.Objects;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CustomerId value must not be blank");
        }
        if (value.length() > 64) {
            throw new IllegalArgumentException("CustomerId value must be <= 64 chars: " + value.length());
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
