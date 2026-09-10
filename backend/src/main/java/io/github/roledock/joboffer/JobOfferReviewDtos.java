package io.github.roledock.joboffer;

import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class JobOfferReviewDtos {
    private JobOfferReviewDtos() {}
    public enum Action { SAVE, BYPASS }
    public enum RequirementSource { LLM_EXTRACTED, USER_ADDED }
    public record Command(@NotNull Action action, @Valid CurrentExtraction extraction) {}
    // The persisted projection is independent of the strict, immutable provider contract.
    public record CurrentExtraction(String company, String position,
            @NotNull @Valid Location location, @NotNull @Valid WorkArrangement workArrangement,
            @NotNull ContractType contractType, String sourceLanguage, String summary,
            @NotNull List<@NotBlank String> missions,
            @NotNull List<@NotNull @Valid CurrentRequirement> requirements) {}
    public record CurrentRequirement(UUID id, RequirementSource source, String rawText,
            @NotBlank String canonicalLabel, @NotNull RequirementCategory category,
            @NotNull RequirementKind requirementKind, @NotNull Centrality centrality,
            @NotNull Explicitness explicitness, @NotNull Boolean hardBlockerCandidate,
            String blockerCondition, @Valid Constraint constraint, ExtractionConfidence extractionConfidence) {}
}
