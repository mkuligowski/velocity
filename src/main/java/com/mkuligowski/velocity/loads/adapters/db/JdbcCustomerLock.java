package com.mkuligowski.velocity.loads.adapters.db;

import com.mkuligowski.velocity.loads.adapters.db.sql.CustomerLockSql;
import com.mkuligowski.velocity.loads.app.ports.CustomerLock;
import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcCustomerLock implements CustomerLock {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    JdbcCustomerLock(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void acquireFor(CustomerId customerId) {
        // Ensure the lock row exists (first time we see this customer).
        var upsertParams = new MapSqlParameterSource()
                .addValue("customer_id", customerId.value())
                .addValue("created_at", clock.instant().atOffset(ZoneOffset.UTC));
        jdbc.update(CustomerLockSql.UPSERT, upsertParams);

        // SELECT ... FOR UPDATE takes an exclusive row lock for the duration of the
        // surrounding @Transactional. Concurrent callers for the same customer block here.
        var lockParams = new MapSqlParameterSource().addValue("customer_id", customerId.value());
        jdbc.queryForList(CustomerLockSql.SELECT_FOR_UPDATE, lockParams);
    }
}
