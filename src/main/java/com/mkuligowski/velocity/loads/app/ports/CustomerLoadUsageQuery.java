package com.mkuligowski.velocity.loads.app.ports;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.policy.CustomerLoadUsage;
import java.time.Instant;

public interface CustomerLoadUsageQuery {

    /**
     * Aggregates the customer's already-accepted loads that fall within the UTC day and
     * ISO week containing {@code atTime}. Declined loads are excluded — they don't consume
     * budget.
     */
    CustomerLoadUsage forCustomerOn(CustomerId customerId, Instant atTime);
}
