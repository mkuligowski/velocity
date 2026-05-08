package com.mkuligowski.velocity.loads.domain.policy;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VelocityPolicyEvaluator {

    private VelocityPolicyEvaluator() {}

    public static Decision evaluate(List<VelocityPolicy> policies, CustomerLoadUsage usage, Money candidate) {
        Objects.requireNonNull(policies, "policies must not be null");
        Objects.requireNonNull(usage, "usage must not be null");
        Objects.requireNonNull(candidate, "candidate must not be null");

        return policies.stream()
                .map(p -> p.check(usage, candidate))
                .flatMap(Optional::stream)
                .findFirst()
                .map(Decision::declined)
                .orElseGet(Decision::accepted);
    }
}
