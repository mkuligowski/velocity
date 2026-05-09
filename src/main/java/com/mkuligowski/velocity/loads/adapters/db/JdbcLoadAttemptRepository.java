package com.mkuligowski.velocity.loads.adapters.db;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mkuligowski.velocity.loads.adapters.db.sql.LoadAttemptSql;
import com.mkuligowski.velocity.loads.app.ports.LoadAttemptRepository;
import com.mkuligowski.velocity.loads.domain.model.LoadAttempt;
import com.mkuligowski.velocity.loads.domain.model.LoadAttemptAccepted;
import com.mkuligowski.velocity.loads.domain.model.LoadAttemptDeclined;
import com.mkuligowski.velocity.loads.domain.model.events.LoadAttemptEvent;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcLoadAttemptRepository implements LoadAttemptRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    JdbcLoadAttemptRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public void save(LoadAttempt attempt) {
        boolean accepted = switch (attempt) {
            case LoadAttemptAccepted ignored -> true;
            case LoadAttemptDeclined ignored -> false;
            default -> throw new IllegalArgumentException(
                    "Cannot save non-terminal LoadAttempt: " + attempt.getClass().getSimpleName());
        };

        if (attempt.events().size() != 1) {
            throw new IllegalStateException(
                    "Terminal LoadAttempt must carry exactly one event, got: " + attempt.events().size());
        }
        LoadAttemptEvent event = attempt.events().get(0);

        insertEvent(event);
        insertSnapshot(attempt, accepted);
    }

    private void insertEvent(LoadAttemptEvent event) {
        var params = new MapSqlParameterSource()
                .addValue("event_id", event.eventId())
                .addValue("customer_id", event.customerId().value())
                .addValue("load_id", event.loadId().value())
                .addValue("event_type", event.type().name())
                .addValue("event_payload", serialize(event))
                .addValue("occurred_at", event.occurredAt().atOffset(ZoneOffset.UTC))
                .addValue("recorded_at", clock.instant().atOffset(ZoneOffset.UTC));
        jdbc.update(LoadAttemptSql.INSERT_EVENT, params);
    }

    private void insertSnapshot(LoadAttempt attempt, boolean accepted) {
        var params = new MapSqlParameterSource()
                .addValue("customer_id", attempt.customerId().value())
                .addValue("load_id", attempt.loadId().value())
                .addValue("amount", attempt.amount().value())
                .addValue("load_time", attempt.loadTime().atOffset(ZoneOffset.UTC))
                .addValue("accepted", accepted);
        jdbc.update(LoadAttemptSql.INSERT_SNAPSHOT, params);
    }

    private String serialize(LoadAttemptEvent event) {
        try {
            return objectMapper.writeValueAsString(LoadAttemptEventPayload.from(event));
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize LoadAttemptEvent payload for event " + event.eventId(), e);
        }
    }
}
