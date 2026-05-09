package com.mkuligowski.velocity.loads.app.ports;

import com.mkuligowski.velocity.loads.domain.model.LoadAttempt;

public interface LoadAttemptRepository {

    /**
     * Persists a terminal {@link LoadAttempt} state and its single triggering event in one
     * transaction: the event is appended to {@code load_attempt_event} and the snapshot row
     * is written to {@code load_attempt_snapshot}.
     *
     * <p>Caller guarantees: idempotency lock has already been acquired for
     * {@code (customerId, loadId)}.
     *
     * @param attempt either {@code LoadAttemptAccepted} or {@code LoadAttemptDeclined}
     */
    void save(LoadAttempt attempt);
}
