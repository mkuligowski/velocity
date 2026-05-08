package com.mkuligowski.velocity.shared.time;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

public final class UtcTimeBuckets {

    private UtcTimeBuckets() {}

    public static Window dayWindow(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        Instant start = at.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return new Window(start, start.plus(1, ChronoUnit.DAYS));
    }

    public static Window weekWindow(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        Instant start = at.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return new Window(start, start.plus(7, ChronoUnit.DAYS));
    }

    public record Window(Instant startInclusive, Instant endExclusive) {
        public Window {
            Objects.requireNonNull(startInclusive);
            Objects.requireNonNull(endExclusive);
            if (!endExclusive.isAfter(startInclusive)) {
                throw new IllegalArgumentException("endExclusive must be after startInclusive");
            }
        }

        public boolean contains(Instant t) {
            return !t.isBefore(startInclusive) && t.isBefore(endExclusive);
        }
    }
}
