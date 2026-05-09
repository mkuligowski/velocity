package com.mkuligowski.velocity.loads.adapters.db;

import com.mkuligowski.velocity.loads.adapters.db.sql.CustomerLoadUsageSql;
import com.mkuligowski.velocity.loads.app.ports.CustomerLoadUsageQuery;
import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.Money;
import com.mkuligowski.velocity.loads.domain.policy.CustomerLoadUsage;
import com.mkuligowski.velocity.shared.time.UtcTimeBuckets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCustomerLoadUsageQuery implements CustomerLoadUsageQuery {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcCustomerLoadUsageQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public CustomerLoadUsage forCustomerOn(CustomerId customerId, Instant atTime) {
        var dayAggregate = aggregate(customerId, UtcTimeBuckets.dayWindow(atTime));
        var weekAggregate = aggregate(customerId, UtcTimeBuckets.weekWindow(atTime));

        return new CustomerLoadUsage(
                new Money(dayAggregate.totalAmount),
                dayAggregate.totalCount,
                new Money(weekAggregate.totalAmount),
                weekAggregate.totalCount);
    }

    private Aggregate aggregate(CustomerId customerId, UtcTimeBuckets.Window window) {
        var params = new MapSqlParameterSource()
                .addValue("customer_id", customerId.value())
                .addValue("window_start", window.startInclusive().atOffset(ZoneOffset.UTC))
                .addValue("window_end", window.endExclusive().atOffset(ZoneOffset.UTC));
        var result = jdbc.queryForObject(
                CustomerLoadUsageSql.SUM_AND_COUNT_IN_WINDOW,
                params,
                (rs, rowNum) -> new Aggregate(
                        rs.getBigDecimal("total_amount"),
                        rs.getInt("total_count")));
        return Objects.requireNonNull(result, "aggregate query unexpectedly returned null");
    }

    private record Aggregate(BigDecimal totalAmount, int totalCount) {}
}
