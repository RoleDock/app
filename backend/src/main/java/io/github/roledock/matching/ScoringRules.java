package io.github.roledock.matching;

import io.github.roledock.joboffer.JobOfferReviewDtos.CurrentRequirement;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import static io.github.roledock.matching.RequirementAssessment.*;
import java.util.Map;

/** Provisional v0 product hypotheses; see docs/architecture/scoring.md. */
final class ScoringRules {
    private ScoringRules() {}
    static final Map<RequirementKind, Double> KIND_WEIGHTS = Map.of(
            RequirementKind.REQUIRED, 4d, RequirementKind.PREFERRED, 1d,
            RequirementKind.CONTEXTUAL, 0d, RequirementKind.UNKNOWN, 0d);
    static final Map<Centrality, Double> CENTRALITY_WEIGHTS = Map.of(
            Centrality.CORE, 2d, Centrality.SUPPORTING, 1d, Centrality.INCIDENTAL, .5d, Centrality.UNKNOWN, 0d);
    static final Map<TransferRelation, Double> TRANSFER_COVERAGE = Map.of(
            TransferRelation.EXACT, 1d, TransferRelation.EQUIVALENT, .75d,
            TransferRelation.ADJACENT, .5d, TransferRelation.PREREQUISITE, .25d, TransferRelation.NONE, 0d);
    static final double EXPERIENCE_SHORTFALL_COVERAGE = .75;
    static final double SEVERE_PARTIAL_MAX = .25;
    static final double STRONG_COVERAGE = 80;
    static final double BRIDGE_COVERAGE = 60;
    static final int SEVERAL_CRITICAL_GAPS = 2;
    static final int MANY_UNCERTAINTIES = 3;
    record Coverage(Double value, String rationale) {}
    static double weight(CurrentRequirement r) {
        return KIND_WEIGHTS.get(r.requirementKind()) * CENTRALITY_WEIGHTS.get(r.centrality());
    }
    static Coverage coverage(CurrentRequirement r, RequirementAssessment a) {
        return switch (a.status()) {
            case MATCH -> new Coverage(1d, "Exigence couverte.");
            case MISSING -> new Coverage(0d, "Exigence non couverte.");
            case UNKNOWN -> new Coverage(null, "Information inconnue : exclue du dénominateur.");
            case NOT_APPLICABLE -> new Coverage(null, "Exigence non applicable : exclue du calcul.");
            case PARTIAL -> partial(r, a);
        };
    }
    private static Coverage partial(CurrentRequirement r, RequirementAssessment a) {
        // EXACT here identifies the linked skill, not sufficient experience duration.
        if (r.category() == RequirementCategory.EXPERIENCE
                && (a.transferRelation() == TransferRelation.EXACT || a.transferRelation() == TransferRelation.NONE)
                && r.constraint() != null && r.constraint().operator() == ConstraintOperator.AT_LEAST
                && a.evidence().stream().anyMatch(e -> e.type() == EvidenceType.EXPERIENCE)) {
            return new Coverage(EXPERIENCE_SHORTFALL_COVERAGE,
                    "Durée proche du minimum : couverture conventionnelle de 0,75 (hypothèse v0, pas un ratio de durée).");
        }
        if (a.transferRelation() != TransferRelation.NONE && a.transferRelation() != TransferRelation.EXACT)
            return new Coverage(TRANSFER_COVERAGE.get(a.transferRelation()), switch (a.transferRelation()) {
                case EQUIVALENT -> "Couverture par équivalence explicite.";
                case ADJACENT -> "Couverture par compétence voisine.";
                case PREREQUISITE -> "Couverture par prérequis.";
                default -> throw new IllegalStateException("Unsupported partial transfer relation");
            });
        return new Coverage(null, "Couverture partielle sans règle numérique applicable : à vérifier, exclue du calcul.");
    }
}
