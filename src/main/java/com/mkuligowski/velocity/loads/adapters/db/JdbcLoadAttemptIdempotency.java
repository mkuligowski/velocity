package com.mkuligowski.velocity.loads.adapters.db;

import com.mkuligowski.velocity.loads.adapters.db.sql.LoadAttemptIdempotencySql;
import com.mkuligowski.velocity.loads.app.ports.LoadAttemptIdempotency;
import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcLoadAttemptIdempotency implements LoadAttemptIdempotency {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcLoadAttemptIdempotency(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public boolean tryLock(CustomerId customerId, LoadId loadId, Instant lockedAt) {
        var params = new MapSqlParameterSource()
                .addValue("customer_id", customerId.value())
                .addValue("load_id", loadId.value())
                .addValue("locked_at", lockedAt.atOffset(ZoneOffset.UTC));
        try {
            jdbc.update(LoadAttemptIdempotencySql.INSERT, params);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }
}
