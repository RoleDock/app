package io.github.roledock.joboffer;

import io.github.roledock.joboffer.extraction.JobOfferExtraction;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class JobOfferDtos {
    private JobOfferDtos() {}

    public record AnalyzeRequest(@NotBlank @Size(max = 50000) String originalText,
                                 @Size(max = 2000) String sourceUrl) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public static AnalyzeRequest fromJson(com.fasterxml.jackson.databind.JsonNode payload) {
            if (!payload.isObject()) throw new IllegalArgumentException("Expected a JSON object.");
            return new AnalyzeRequest(stringValue(payload, "originalText"), stringValue(payload, "sourceUrl"));
        }

        private static String stringValue(com.fasterxml.jackson.databind.JsonNode payload, String field) {
            var value = payload.get(field);
            if (value == null || value.isNull()) return null;
            if (!value.isTextual()) throw new IllegalArgumentException("Expected a string.");
            return value.textValue();
        }
    }
    public record SaveRequest(@NotNull UUID analysisId) {}
    public record Analysis(UUID analysisId, Instant analyzedAt, JobOfferExtraction extraction) {}
    public record Response(UUID id, String originalText, String sourceUrl, Instant analyzedAt,
                           ReviewStatus reviewStatus, Instant reviewBypassedAt, JobOfferReviewDtos.CurrentExtraction extraction) {}
}
