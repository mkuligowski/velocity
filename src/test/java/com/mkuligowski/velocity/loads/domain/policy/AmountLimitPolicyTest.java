package com.mkuligowski.velocity.loads.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AmountLimitPolicyTest {

    private static final PolicyId ID = PolicyId.of(UUID.randomUUID());

    @Nested
    class DailyAmountLimit {

        private final AmountLimitPolicy policy = new AmountLimitPolicy(
                ID, TimeWindow.DAY, Money.parseDollars("$5000.00"), DeclineReason.DAILY_AMOUNT);

        @Test
        void accepts_when_under_limit() {
            var usage = new CustomerLoadUsage(Money.parseDollars("$1000.00"), 1, Money.ZERO, 0);
            assertThat(policy.check(usage, Money.parseDollars("$500.00"))).isEmpty();
        }

        @Test
        void accepts_at_exact_limit() {
            // $4500 already + $500 candidate = $5000 — exactly the limit, not over.
            var usage = new CustomerLoadUsage(Money.parseDollars("$4500.00"), 1, Money.ZERO, 0);
            assertThat(policy.check(usage, Money.parseDollars("$500.00"))).isEmpty();
        }

        @Test
        void declines_when_over_limit_by_one_cent() {
            var usage = new CustomerLoadUsage(Money.parseDollars("$4999.99"), 1, Money.ZERO, 0);
            assertThat(policy.check(usage, Money.parseDollars("$0.02")))
                    .contains(DeclineReason.DAILY_AMOUNT);
        }

        @Test
        void declines_when_already_at_limit_and_anything_added() {
            var usage = new CustomerLoadUsage(Money.parseDollars("$5000.00"), 1, Money.ZERO, 0);
            assertThat(policy.check(usage, Money.parseDollars("$0.01")))
                    .contains(DeclineReason.DAILY_AMOUNT);
        }

        @Test
        void weekly_columns_irrelevant_for_daily_policy() {
            // Daily policy ignores weeklySum — even huge weekly usage shouldn't block a daily check
            var usage = new CustomerLoadUsage(
                    Money.parseDollars("$100.00"), 1,
                    Money.parseDollars("$19000.00"), 5);
            assertThat(policy.check(usage, Money.parseDollars("$100.00"))).isEmpty();
        }
    }

    @Nested
    class WeeklyAmountLimit {

        private final AmountLimitPolicy policy = new AmountLimitPolicy(
                ID, TimeWindow.WEEK, Money.parseDollars("$20000.00"), DeclineReason.WEEKLY_AMOUNT);

        @Test
        void accepts_at_exact_weekly_limit() {
            var usage = new CustomerLoadUsage(Money.ZERO, 0, Money.parseDollars("$15000.00"), 5);
            assertThat(policy.check(usage, Money.parseDollars("$5000.00"))).isEmpty();
        }

        @Test
        void declines_when_weekly_total_exceeds_limit() {
            var usage = new CustomerLoadUsage(Money.ZERO, 0, Money.parseDollars("$19999.99"), 5);
            assertThat(policy.check(usage, Money.parseDollars("$0.02")))
                    .contains(DeclineReason.WEEKLY_AMOUNT);
        }
    }
}
