package com.mkuligowski.velocity.loads.adapters.rest.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @JsonProperty("error") String error,
        @JsonProperty("details") List<Map<String, String>> details,
        @JsonProperty("correlation_id") @Nullable String correlationId
) {

    public static ApiError validation(List<Map<String, String>> details, @Nullable String correlationId) {
        return new ApiError("VALIDATION_FAILED", details, correlationId);
    }

    public static ApiError validation(String message, @Nullable String correlationId) {
        return new ApiError("VALIDATION_FAILED", List.of(Map.of("message", message)), correlationId);
    }

    public static ApiError internal(@Nullable String correlationId) {
        return new ApiError("INTERNAL_ERROR", List.of(), correlationId);
    }
}
