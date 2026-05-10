package com.mkuligowski.velocity.loads.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Nested
    class ParseDollars {

        @Test
        void parses_zero() {
            assertThat(Money.parseDollars("$0.00").value()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void parses_typical_amount() {
            assertThat(Money.parseDollars("$123.45").value())
                    .isEqualByComparingTo(new BigDecimal("123.45"));
        }

        @Test
        void parses_large_amount() {
            assertThat(Money.parseDollars("$99999.99").value())
                    .isEqualByComparingTo(new BigDecimal("99999.99"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1.00",      // missing $
                "$1",        // missing decimals
                "$1.0",      // 1 decimal
                "$1.000",    // 3 decimals
                "$-1.00",    // negative
                "-$1.00",    // negative
                "$.50",      // missing leading digit
                "USD 1.00",  // wrong currency marker
                "$1,234.56", // grouping separator
                "",          // empty
                "$"          // just symbol
        })
        void rejects_malformed(String input) {
            assertThatThrownBy(() -> Money.parseDollars(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> Money.parseDollars(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Constructor {

        @Test
        void rejects_negative() {
            assertThatThrownBy(() -> new Money(new BigDecimal("-0.01")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void zero_is_allowed() {
            assertThat(new Money(BigDecimal.ZERO).value()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void scale_normalised_to_4() {
            assertThat(new Money(new BigDecimal("100.50")).value().scale()).isEqualTo(4);
        }
    }

    @Nested
    class Add {

        @Test
        void adds_two_values() {
            Money sum = Money.parseDollars("$10.00").add(Money.parseDollars("$5.00"));
            assertThat(sum.value()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        void adds_zero_returns_same_value() {
            Money sum = Money.parseDollars("$10.00").add(Money.ZERO);
            assertThat(sum.value()).isEqualByComparingTo(new BigDecimal("10.00"));
        }
    }

    @Nested
    class Comparison {

        @Test
        void compares_by_value_not_scale() {
            // 100 vs 100.00 vs 100.0000 — all equal by compareTo
            Money a = new Money(new BigDecimal("100"));
            Money b = Money.parseDollars("$100.00");
            assertThat(a).isEqualByComparingTo(b);
        }

        @Test
        void greater_returns_positive() {
            assertThat(Money.parseDollars("$5.01").compareTo(Money.parseDollars("$5.00")))
                    .isPositive();
        }

        @Test
        void smaller_returns_negative() {
            assertThat(Money.parseDollars("$4.99").compareTo(Money.parseDollars("$5.00")))
                    .isNegative();
        }
    }
}
