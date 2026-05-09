package com.mkuligowski.velocity.loads.adapters.rest;

import com.mkuligowski.velocity.loads.adapters.rest.dto.LoadRequestDto;
import com.mkuligowski.velocity.loads.app.EvaluateLoadAttemptCommand;
import com.mkuligowski.velocity.loads.domain.model.CustomerId;
import com.mkuligowski.velocity.loads.domain.model.LoadId;

final class LoadRequestMapper {

    private LoadRequestMapper() {}

    static EvaluateLoadAttemptCommand toCommand(LoadRequestDto dto) {
        return new EvaluateLoadAttemptCommand(
                LoadId.of(dto.id()),
                CustomerId.of(dto.customerId()),
                dto.loadAmount(),
                dto.time());
    }
}
