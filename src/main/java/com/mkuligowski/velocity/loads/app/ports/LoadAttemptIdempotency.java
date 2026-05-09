package com.mkuligowski.velocity.loads.app.ports;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import java.time.Instant;

public interface LoadAttemptIdempotency {

    /**
     * Race-safe duplicate-detection gate. Inserts a row into
     * {@code load_attempt_idempotency}; the unique PK on {@code (customer_id, load_id)}
     * means at most one caller wins.
     *
     * @return {@code true} if this caller acquired the lock (first instance);
     *         {@code false} if this {@code (customerId, loadId)} pair was already observed.
     */
    boolean tryLock(CustomerId customerId, LoadId loadId, Instant lockedAt);
}
