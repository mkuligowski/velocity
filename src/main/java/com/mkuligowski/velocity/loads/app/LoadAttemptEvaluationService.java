package com.mkuligowski.velocity.loads.app;

import com.mkuligowski.velocity.loads.app.ports.CustomerLoadUsageQuery;
import com.mkuligowski.velocity.loads.app.ports.LoadAttemptIdempotency;
import com.mkuligowski.velocity.loads.app.ports.LoadAttemptRepository;
import com.mkuligowski.velocity.loads.app.ports.VelocityPolicyRepository;
import com.mkuligowski.velocity.loads.domain.model.LoadAttempt;
import com.mkuligowski.velocity.loads.domain.model.LoadAttemptInitiated;
import com.mkuligowski.velocity.loads.domain.policy.CustomerLoadUsage;
import com.mkuligowski.velocity.loads.domain.policy.Decision;
import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicy;
import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicyEvaluator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoadAttemptEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(LoadAttemptEvaluationService.class);

    private final LoadAttemptRepository loadAttemptRepository;
    private final LoadAttemptIdempotency idempotency;
    private final VelocityPolicyRepository policyRepository;
    private final CustomerLoadUsageQuery usageQuery;
    private final Clock clock;

    public LoadAttemptEvaluationService(
            LoadAttemptRepository loadAttemptRepository,
            LoadAttemptIdempotency idempotency,
            VelocityPolicyRepository policyRepository,
            CustomerLoadUsageQuery usageQuery,
            Clock clock) {
        this.loadAttemptRepository = Objects.requireNonNull(loadAttemptRepository);
        this.idempotency = Objects.requireNonNull(idempotency);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.usageQuery = Objects.requireNonNull(usageQuery);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public Optional<LoadAttempt> evaluate(EvaluateLoadAttemptCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");
        Instant now = clock.instant();

        if (!idempotency.tryLock(cmd.customerId(), cmd.loadId(), now)) {
            log.info("Duplicate load attempt skipped: customer={} load={}",
                    cmd.customerId().value(), cmd.loadId().value());
            return Optional.empty();
        }

        LoadAttemptInitiated initiated = LoadAttempt.initiate(
                cmd.loadId(), cmd.customerId(), cmd.amount(), cmd.loadTime());

        List<VelocityPolicy> policies = policyRepository.findAll();
        CustomerLoadUsage usage = usageQuery.forCustomerOn(cmd.customerId(), cmd.loadTime());
        Decision decision = VelocityPolicyEvaluator.evaluate(policies, usage, cmd.amount());

        UUID eventId = UUID.randomUUID();
        LoadAttempt nextState = switch (decision) {
            case Decision.Accepted ignored -> initiated.accept(eventId, now).state();
            case Decision.Declined declined -> initiated.decline(declined.reason(), eventId, now).state();
        };

        loadAttemptRepository.save(nextState);

        if (log.isInfoEnabled()) {
            log.info("Load attempt decided: customer={} load={} decision={} dailySum={} dailyCount={} weeklySum={}",
                    cmd.customerId().value(),
                    cmd.loadId().value(),
                    decision instanceof Decision.Accepted ? "ACCEPTED"
                            : "DECLINED:" + ((Decision.Declined) decision).reason(),
                    usage.dailySum().value(),
                    usage.dailyCount(),
                    usage.weeklySum().value());
        }

        return Optional.of(nextState);
    }
}
