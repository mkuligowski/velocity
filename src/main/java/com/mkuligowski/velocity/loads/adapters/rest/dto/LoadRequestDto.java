package com.mkuligowski.velocity.loads.adapters.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mkuligowski.velocity.loads.adapters.rest.MoneyDeserializer;
import com.mkuligowski.velocity.loads.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import tools.jackson.databind.annotation.JsonDeserialize;

public record LoadRequestDto(
        @NotBlank @JsonProperty("id") String id,
        @NotBlank @JsonProperty("customer_id") String customerId,
        @NotNull @JsonProperty("load_amount") @JsonDeserialize(using = MoneyDeserializer.class) Money loadAmount,
        @NotNull @JsonProperty("time") Instant time
) {}
