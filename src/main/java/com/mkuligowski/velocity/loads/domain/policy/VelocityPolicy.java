package com.mkuligowski.velocity.loads.domain.policy;

import com.mkuligowski.velocity.loads.domain.model.Money;
import java.util.Optional;

public sealed interface VelocityPolicy
        permits AmountLimitPolicy, CountLimitPolicy {

    PolicyId id();

    TimeWindow window();

    DeclineReason declineReason();

    Optional<DeclineReason> check(CustomerLoadUsage usage, Money candidate);
}
