package com.mkuligowski.velocity.loads.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

public record Money(BigDecimal value) implements Comparable<Money> {

    private static final Pattern DOLLAR_FORMAT = Pattern.compile("^\\$\\d+\\.\\d{2}$");
    private static final int STORAGE_SCALE = 4;

    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY));

    public Money {
        Objects.requireNonNull(value, "Money value must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Money value must be non-negative: " + value);
        }
        value = value.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money parseDollars(String dollarFormatted) {
        Objects.requireNonNull(dollarFormatted, "dollarFormatted must not be null");
        if (!DOLLAR_FORMAT.matcher(dollarFormatted).matches()) {
            throw new IllegalArgumentException(
                    "Invalid money format: '" + dollarFormatted + "' (expected $X.XX)");
        }
        return new Money(new BigDecimal(dollarFormatted.substring(1)));
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(value.add(other.value));
    }

    @Override
    public int compareTo(Money other) {
        return value.compareTo(other.value);
    }
}
