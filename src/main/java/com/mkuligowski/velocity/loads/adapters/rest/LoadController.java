package com.mkuligowski.velocity.loads.adapters.rest;

import com.mkuligowski.velocity.loads.adapters.rest.dto.LoadDecisionDto;
import com.mkuligowski.velocity.loads.adapters.rest.dto.LoadRequestDto;
import com.mkuligowski.velocity.loads.app.LoadAttemptEvaluationService;
import com.mkuligowski.velocity.loads.domain.model.LoadAttempt;
import com.mkuligowski.velocity.loads.domain.model.LoadAttemptAccepted;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loads")
public class LoadController {

    private final LoadAttemptEvaluationService evaluationService;

    public LoadController(LoadAttemptEvaluationService evaluationService) {
        this.evaluationService = Objects.requireNonNull(evaluationService);
    }

    @PostMapping
    public ResponseEntity<LoadDecisionDto> handle(@Valid @RequestBody LoadRequestDto request) {
        Optional<LoadAttempt> result = evaluationService.evaluate(LoadRequestMapper.toCommand(request));
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        LoadAttempt attempt = result.get();
        boolean accepted = attempt instanceof LoadAttemptAccepted;
        return ResponseEntity.ok(new LoadDecisionDto(
                attempt.loadId().value(),
                attempt.customerId().value(),
                accepted));
    }
}
