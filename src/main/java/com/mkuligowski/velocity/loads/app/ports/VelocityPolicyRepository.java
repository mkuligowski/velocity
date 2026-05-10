package com.mkuligowski.velocity.loads.app.ports;

import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicy;
import java.util.List;

public interface VelocityPolicyRepository {

    /**
     * Returns all currently-active velocity policies. Policies are global today — no
     * per-customer overrides. No caching — runs on every load attempt.
     */
    List<VelocityPolicy> findAll();
}
