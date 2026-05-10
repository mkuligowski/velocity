package com.mkuligowski.velocity.loads.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CountLimitPolicyTest {

    private static final PolicyId ID = PolicyId.of(UUID.randomUUID());

    private final CountLimitPolicy policy = new CountLimitPolicy(
            ID, TimeWindow.DAY, 3, DeclineReason.DAILY_COUNT);

    @Test
    void accepts_with_zero_prior_loads() {
        var usage = new CustomerLoadUsage(Money.ZERO, 0, Money.ZERO, 0);
        assertThat(policy.check(usage, Money.parseDollars("$1.00"))).isEmpty();
    }

    @Test
    void accepts_at_two_prior_loads() {
        var usage = new CustomerLoadUsage(Money.ZERO, 2, Money.ZERO, 2);
        assertThat(policy.check(usage, Money.parseDollars("$1.00"))).isEmpty();
    }

    @Test
    void declines_at_three_prior_loads() {
        // Spec: "≤ 3 accepted loads per day" — the 4th attempt is the one that's declined,
        // i.e. when count >= 3 at evaluation time, the candidate would be the 4th.
        var usage = new CustomerLoadUsage(Money.ZERO, 3, Money.ZERO, 3);
        assertThat(policy.check(usage, Money.parseDollars("$1.00")))
                .contains(DeclineReason.DAILY_COUNT);
    }

    @Test
    void declines_at_more_than_three() {
        var usage = new CustomerLoadUsage(Money.ZERO, 10, Money.ZERO, 10);
        assertThat(policy.check(usage, Money.parseDollars("$1.00")))
                .contains(DeclineReason.DAILY_COUNT);
    }

    @Test
    void candidate_amount_does_not_affect_count_check() {
        var usageBelow = new CustomerLoadUsage(Money.ZERO, 2, Money.ZERO, 2);
        var usageAt = new CustomerLoadUsage(Money.ZERO, 3, Money.ZERO, 3);
        Money tinyAmount = Money.parseDollars("$0.01");
        Money bigAmount = Money.parseDollars("$10000.00");

        assertThat(policy.check(usageBelow, tinyAmount)).isEmpty();
        assertThat(policy.check(usageBelow, bigAmount)).isEmpty();
        assertThat(policy.check(usageAt, tinyAmount)).contains(DeclineReason.DAILY_COUNT);
        assertThat(policy.check(usageAt, bigAmount)).contains(DeclineReason.DAILY_COUNT);
    }
}
