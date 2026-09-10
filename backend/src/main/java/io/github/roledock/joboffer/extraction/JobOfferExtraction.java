package io.github.roledock.joboffer.extraction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record JobOfferExtraction(
        String company, String position,
        @NotNull @Valid Location location,
        @NotNull @Valid WorkArrangement workArrangement,
        @NotNull ContractType contractType,
        String sourceLanguage, String summary,
        @NotNull List<@NotBlank String> missions,
        @NotNull List<@NotNull @Valid Requirement> requirements) {

    public record Location(String city, String region, String country) {}
    public record WorkArrangement(@NotNull WorkArrangementType type, String remoteArea,
                                  @Min(0) @Max(7) Integer onSiteDaysPerWeek) {}
    public record Requirement(
            @NotBlank String rawText, @NotBlank String canonicalLabel,
            @NotNull RequirementCategory category, @NotNull RequirementKind requirementKind,
            @NotNull Centrality centrality, @NotNull Explicitness explicitness,
            @NotNull Boolean hardBlockerCandidate, String blockerCondition,
            @Valid Constraint constraint, @NotNull ExtractionConfidence extractionConfidence) {}
    public record Constraint(@NotNull ConstraintOperator operator, @NotBlank String value, String unit) {}

    public enum WorkArrangementType { ONSITE, HYBRID, REMOTE, UNKNOWN }
    public enum ContractType { PERMANENT, FIXED_TERM, FREELANCE, INTERNSHIP, APPRENTICESHIP, TEMPORARY, OTHER, UNKNOWN }
    public enum RequirementCategory {
        TECH_SKILL, EXPERIENCE, DOMAIN_KNOWLEDGE, TITLE_LEVEL, EDUCATION, CERTIFICATION,
        LOCATION, WORK_AUTHORIZATION, LANGUAGE, AVAILABILITY, CONTRACT, SOFT_SKILL, OTHER
    }
    public enum RequirementKind { REQUIRED, PREFERRED, CONTEXTUAL, UNKNOWN }
    public enum Centrality { CORE, SUPPORTING, INCIDENTAL, UNKNOWN }
    public enum Explicitness { EXPLICIT, INFERRED }
    public enum ExtractionConfidence { HIGH, MEDIUM, LOW }
    public enum ConstraintOperator { AT_LEAST, AT_MOST, EQUALS, RANGE, OTHER }
}
