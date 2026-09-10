package io.github.roledock.joboffer.extraction;

import io.github.roledock.api.error.ApiError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferExtractionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobOfferExtractionController.class);
    private final JobOfferExtractor extractor;

    public JobOfferExtractionController(JobOfferExtractor extractor) {
        this.extractor = extractor;
    }

    public record Request(@NotBlank @Size(max = 50000) String text) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public static Request fromJson(com.fasterxml.jackson.databind.JsonNode payload) {
            if (!payload.isObject()) throw new IllegalArgumentException("Expected a JSON object.");
            var text = payload.get("text");
            if (text != null && !text.isNull() && !text.isTextual()) {
                throw new IllegalArgumentException("Expected text to be a string.");
            }
            return new Request(text == null || text.isNull() ? null : text.textValue());
        }
    }

    @PostMapping("/extract")
    public JobOfferExtraction extract(@Valid @RequestBody Request request) {
        return extractor.extract(request.text());
    }

    @ExceptionHandler(ExtractionException.class)
    ResponseEntity<ApiError> extractionFailure(ExtractionException exception) {
        // Only a fixed diagnostic code: no exception message, cause, provider body or input.
        log.warn("Job-offer extraction failed: reason={} providerStatus={}", exception.reason(), exception.providerStatus());
        return ResponseEntity.status(exception.isUnavailable() ? 503 : 502)
                .body(new ApiError("EXTRACTION_FAILED", exception.getMessage(), Map.of()));
    }
}
