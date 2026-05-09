package com.mkuligowski.velocity.loads.adapters.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoadDecisionDto(
        @JsonProperty("id") String id,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("accepted") boolean accepted
) {}
