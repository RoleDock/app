package io.github.roledock.matching;

import io.github.roledock.joboffer.ReviewStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** On-demand DTO. A null score means no evaluable weighted requirement. */
public record OfferAnalysis(LocalDate assessedOn, ReviewStatus reviewStatus, Double coverageScore,
        Eligibility eligibility, Recommendation recommendation, List<CriticalGap> criticalGaps,
        Uncertainty uncertainty, List<RequirementAssessment> requirementAssessments,
        List<Contribution> contributions) {
    public OfferAnalysis {
        criticalGaps = List.copyOf(criticalGaps);
        requirementAssessments = List.copyOf(requirementAssessments);
        contributions = List.copyOf(contributions);
    }
    public enum Eligibility { ELIGIBLE, ELIGIBLE_WITH_CONSTRAINT, NOT_ELIGIBLE, UNKNOWN }
    public enum Recommendation { APPLY_NOW, APPLY_WITH_BRIDGE, STRETCH, VERIFY_FIRST, SKIP_CONFIRMED_BLOCKER }
    public enum UncertaintyLevel { LOW, MEDIUM, HIGH }
    public record CriticalGap(UUID requirementId, String label, RequirementAssessment.Status status, String rationale) {}
    public record Uncertainty(UncertaintyLevel level, List<String> reasons) {
        public Uncertainty { reasons = List.copyOf(reasons); }
    }
    public record Contribution(UUID requirementId, double weight, Double coverage, boolean included,
                               UUID duplicateOf, String rationale) {}
}
