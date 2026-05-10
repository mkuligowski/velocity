package com.mkuligowski.velocity.loads.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VelocityPolicyEvaluatorTest {

    private static final PolicyId DAILY_AMOUNT_ID = PolicyId.of(UUID.randomUUID());
    private static final PolicyId WEEKLY_AMOUNT_ID = PolicyId.of(UUID.randomUUID());
    private static final PolicyId DAILY_COUNT_ID = PolicyId.of(UUID.randomUUID());

    private static final List<VelocityPolicy> SPEC_POLICIES = List.of(
            new AmountLimitPolicy(DAILY_AMOUNT_ID, TimeWindow.DAY,
                    Money.parseDollars("$5000.00"), DeclineReason.DAILY_AMOUNT),
            new AmountLimitPolicy(WEEKLY_AMOUNT_ID, TimeWindow.WEEK,
                    Money.parseDollars("$20000.00"), DeclineReason.WEEKLY_AMOUNT),
            new CountLimitPolicy(DAILY_COUNT_ID, TimeWindow.DAY, 3, DeclineReason.DAILY_COUNT));

    @Test
    void empty_policy_list_accepts_everything() {
        var decision = VelocityPolicyEvaluator.evaluate(
                List.of(),
                CustomerLoadUsage.empty(),
                Money.parseDollars("$1000000.00"));
        assertThat(decision).isInstanceOf(Decision.Accepted.class);
    }

    @Test
    void all_policies_pass_under_limits() {
        var usage = new CustomerLoadUsage(
                Money.parseDollars("$1000.00"), 1,
                Money.parseDollars("$5000.00"), 1);
        var decision = VelocityPolicyEvaluator.evaluate(
                SPEC_POLICIES, usage, Money.parseDollars("$500.00"));
        assertThat(decision).isInstanceOf(Decision.Accepted.class);
    }

    @Test
    void daily_amount_breach_declines_with_DAILY_AMOUNT() {
        var usage = new CustomerLoadUsage(
                Money.parseDollars("$4500.00"), 1,
                Money.parseDollars("$4500.00"), 1);
        var decision = VelocityPolicyEvaluator.evaluate(
                SPEC_POLICIES, usage, Money.parseDollars("$501.00"));
        assertThat(decision).isInstanceOf(Decision.Declined.class)
                .extracting(d -> ((Decision.Declined) d).reason())
                .isEqualTo(DeclineReason.DAILY_AMOUNT);
    }

    @Test
    void daily_count_breach_declines_with_DAILY_COUNT() {
        var usage = new CustomerLoadUsage(
                Money.parseDollars("$100.00"), 3,
                Money.parseDollars("$100.00"), 3);
        var decision = VelocityPolicyEvaluator.evaluate(
                SPEC_POLICIES, usage, Money.parseDollars("$1.00"));
        assertThat(decision).isInstanceOf(Decision.Declined.class)
                .extracting(d -> ((Decision.Declined) d).reason())
                .isEqualTo(DeclineReason.DAILY_COUNT);
    }

    @Test
    void weekly_amount_breach_declines_with_WEEKLY_AMOUNT() {
        // Daily under $5k, count under 3, but weekly over $20k.
        var usage = new CustomerLoadUsage(
                Money.parseDollars("$1000.00"), 2,
                Money.parseDollars("$19500.00"), 4);
        var decision = VelocityPolicyEvaluator.evaluate(
                SPEC_POLICIES, usage, Money.parseDollars("$501.00"));
        assertThat(decision).isInstanceOf(Decision.Declined.class)
                .extracting(d -> ((Decision.Declined) d).reason())
                .isEqualTo(DeclineReason.WEEKLY_AMOUNT);
    }

    @Test
    void first_matching_policy_wins() {
        // Both daily-amount AND weekly-amount would breach. Daily comes first → DAILY_AMOUNT wins.
        var usage = new CustomerLoadUsage(
                Money.parseDollars("$4900.00"), 1,
                Money.parseDollars("$19900.00"), 1);
        var decision = VelocityPolicyEvaluator.evaluate(
                SPEC_POLICIES, usage, Money.parseDollars("$200.00"));
        assertThat(decision).isInstanceOf(Decision.Declined.class)
                .extracting(d -> ((Decision.Declined) d).reason())
                .isEqualTo(DeclineReason.DAILY_AMOUNT);
    }
}
