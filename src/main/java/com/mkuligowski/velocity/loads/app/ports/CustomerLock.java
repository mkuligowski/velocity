package com.mkuligowski.velocity.loads.app.ports;

import com.mkuligowski.velocity.loads.domain.model.CustomerId;

public interface CustomerLock {

    /**
     * Acquires an exclusive lock on the given {@link CustomerId} for the lifetime of the
     * current transaction. Concurrent callers for the same customer (on this pod or any
     * other) block here until the lock-holder commits or rolls back.
     *
     * <p>This is what makes the velocity-limit checks safe under multi-pod concurrent
     * traffic: two attempts for the same customer cannot both pass the daily-total read
     * and both INSERT, because the second one waits until the first has committed (or
     * been rolled back) and re-reads the now-updated total.
     */
    void acquireFor(CustomerId customerId);
}
