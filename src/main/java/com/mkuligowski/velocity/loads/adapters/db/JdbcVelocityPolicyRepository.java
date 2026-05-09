package com.mkuligowski.velocity.loads.adapters.db;

import com.mkuligowski.velocity.loads.adapters.db.sql.VelocityPolicySql;
import com.mkuligowski.velocity.loads.app.ports.VelocityPolicyRepository;
import com.mkuligowski.velocity.loads.domain.policy.VelocityPolicy;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcVelocityPolicyRepository implements VelocityPolicyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcVelocityPolicyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public List<VelocityPolicy> findAll() {
        return jdbc.query(VelocityPolicySql.FIND_ALL, VelocityPolicyRowMapper.INSTANCE);
    }
}
