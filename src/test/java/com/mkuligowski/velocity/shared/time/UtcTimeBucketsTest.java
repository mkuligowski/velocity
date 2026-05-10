package com.mkuligowski.velocity.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UtcTimeBucketsTest {

    @Nested
    class DayWindow {

        @Test
        void at_midnight_starts_same_day() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-02T00:00:00Z"));
        }

        @Test
        void at_noon_stays_in_same_day() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-01-01T12:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-02T00:00:00Z"));
        }

        @Test
        void one_second_before_midnight_is_in_current_day() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-01-01T23:59:59Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-02T00:00:00Z"));
        }

        @Test
        void crossing_year_boundary() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-12-31T23:30:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-12-31T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2019-01-01T00:00:00Z"));
        }
    }

    @Nested
    class WeekWindow {

        @Test
        void monday_starts_its_own_week() {
            // 2018-01-01 was a Monday
            var w = UtcTimeBuckets.weekWindow(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-08T00:00:00Z"));
        }

        @Test
        void wednesday_belongs_to_week_starting_monday() {
            var w = UtcTimeBuckets.weekWindow(Instant.parse("2018-01-03T15:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-08T00:00:00Z"));
        }

        @Test
        void sunday_2359_is_in_same_week_as_previous_monday() {
            var w = UtcTimeBuckets.weekWindow(Instant.parse("2018-01-07T23:59:59Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-08T00:00:00Z"));
        }

        @Test
        void next_monday_starts_a_new_week() {
            var w = UtcTimeBuckets.weekWindow(Instant.parse("2018-01-08T00:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-01-08T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2018-01-15T00:00:00Z"));
        }

        @Test
        void week_can_cross_year_boundary() {
            // 2018-12-31 was a Monday — the week 2018-12-31 → 2019-01-06
            var w = UtcTimeBuckets.weekWindow(Instant.parse("2019-01-02T12:00:00Z"));
            assertThat(w.startInclusive()).isEqualTo(Instant.parse("2018-12-31T00:00:00Z"));
            assertThat(w.endExclusive()).isEqualTo(Instant.parse("2019-01-07T00:00:00Z"));
        }
    }

    @Nested
    class WindowContains {

        @Test
        void start_inclusive() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.contains(w.startInclusive())).isTrue();
        }

        @Test
        void end_exclusive() {
            var w = UtcTimeBuckets.dayWindow(Instant.parse("2018-01-01T00:00:00Z"));
            assertThat(w.contains(w.endExclusive())).isFalse();
        }
    }
}
