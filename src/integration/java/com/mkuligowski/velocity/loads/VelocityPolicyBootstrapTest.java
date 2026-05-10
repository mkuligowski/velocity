package com.mkuligowski.velocity.loads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.mkuligowski.velocity.loads.app.ports.VelocityPolicyRepository;
import com.mkuligowski.velocity.loads.domain.model.Money;
import com.mkuligowski.velocity.loads.domain.policy.AmountLimitPolicy;
import com.mkuligowski.velocity.loads.domain.policy.CountLimitPolicy;
import com.mkuligowski.velocity.loads.domain.policy.DeclineReason;
import com.mkuligowski.velocity.loads.domain.policy.TimeWindow;
import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The Liquibase seed changeset must produce exactly the three policies the take-home spec
 * requires. This test is the safety net against a typo in the seed data silently breaking
 * the replay test in {@link LoadEndToEndIntegrationTest}.
 */
@SpringBootTest
class VelocityPolicyBootstrapTest {

    @Autowired private VelocityPolicyRepository policyRepository;

    @Test
    void seeds_exactly_the_three_spec_policies() {
        List<VelocityPolicy> policies = policyRepository.findAll();

        assertThat(policies)
                .extracting(VelocityPolicy::declineReason, VelocityPolicy::window, VelocityPolicy::getClass)
                .containsExactlyInAnyOrder(
                        tuple(DeclineReason.DAILY_AMOUNT, TimeWindow.DAY, AmountLimitPolicy.class),
                        tuple(DeclineReason.WEEKLY_AMOUNT, TimeWindow.WEEK, AmountLimitPolicy.class),
                        tuple(DeclineReason.DAILY_COUNT, TimeWindow.DAY, CountLimitPolicy.class));
    }

    @Test
    void daily_amount_threshold_is_5000_USD() {
        AmountLimitPolicy daily = (AmountLimitPolicy) policyOfReason(DeclineReason.DAILY_AMOUNT);
        assertThat(daily.threshold()).isEqualByComparingTo(Money.parseDollars("$5000.00"));
    }

    @Test
    void weekly_amount_threshold_is_20000_USD() {
        AmountLimitPolicy weekly = (AmountLimitPolicy) policyOfReason(DeclineReason.WEEKLY_AMOUNT);
        assertThat(weekly.threshold()).isEqualByComparingTo(Money.parseDollars("$20000.00"));
    }

    @Test
    void daily_count_threshold_is_3() {
        CountLimitPolicy count = (CountLimitPolicy) policyOfReason(DeclineReason.DAILY_COUNT);
        assertThat(count.threshold()).isEqualTo(3);
    }

    private VelocityPolicy policyOfReason(DeclineReason reason) {
        return policyRepository.findAll().stream()
                .filter(p -> p.declineReason() == reason)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No policy seeded for reason: " + reason));
    }
}
