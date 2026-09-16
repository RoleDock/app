package io.github.roledock.matching;

import java.util.List;
import java.util.UUID;

/** Recomputed evidence assessment, never a persisted score. */
public record RequirementAssessment(UUID requirementId, Status status, TransferRelation transferRelation,
        EvidenceStrength evidenceStrength, AssessmentConfidence assessmentConfidence,
        EligibilityEffect eligibilityEffect, List<EvidenceReference> evidence, String rationale, String attention) {
    public RequirementAssessment { evidence = List.copyOf(evidence); }
    public enum Status { MATCH, PARTIAL, MISSING, UNKNOWN, NOT_APPLICABLE }
    public enum TransferRelation { EXACT, EQUIVALENT, ADJACENT, PREREQUISITE, NONE }
    public enum EvidenceStrength { STRONG, MODERATE, WEAK, NONE }
    public enum AssessmentConfidence { HIGH, MEDIUM, LOW }
    public enum EligibilityEffect { BLOCK, POSSIBLE_BLOCK, NONE }
    // Achievements have no stable IDs; projects have no structured skill links in v0.
    public enum EvidenceType { PROFILE_SKILL, EXPERIENCE, EDUCATION, LANGUAGE, CERTIFICATION, CERTIFICATION_LIST, SIGNIFICANT_PROJECT }
    public record EvidenceReference(EvidenceType type, UUID id, String label) {}
}
