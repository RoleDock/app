export interface RequirementAssessment {
  requirementId: string;
  status: 'MATCH' | 'PARTIAL' | 'MISSING' | 'UNKNOWN' | 'NOT_APPLICABLE';
  transferRelation: 'EXACT' | 'EQUIVALENT' | 'ADJACENT' | 'PREREQUISITE' | 'NONE';
  evidenceStrength: 'STRONG' | 'MODERATE' | 'WEAK' | 'NONE';
  assessmentConfidence: 'HIGH' | 'MEDIUM' | 'LOW';
  eligibilityEffect: 'BLOCK' | 'POSSIBLE_BLOCK' | 'NONE';
  evidence: { type: 'PROFILE_SKILL' | 'EXPERIENCE' | 'EDUCATION' | 'LANGUAGE' | 'CERTIFICATION' | 'CERTIFICATION_LIST' | 'SIGNIFICANT_PROJECT'; id: string; label: string }[];
  rationale: string;
  attention: string | null;
}
export interface AssessmentResponse {
  assessedOn: string;
  assessments: RequirementAssessment[];
}

export interface OfferAnalysis {
  assessedOn: string;
  reviewStatus: 'UNREVIEWED' | 'CONFIRMED' | 'CORRECTED';
  coverageScore: number | null;
  eligibility: 'ELIGIBLE' | 'ELIGIBLE_WITH_CONSTRAINT' | 'NOT_ELIGIBLE' | 'UNKNOWN';
  recommendation: 'APPLY_NOW' | 'APPLY_WITH_BRIDGE' | 'STRETCH' | 'VERIFY_FIRST' | 'SKIP_CONFIRMED_BLOCKER';
  criticalGaps: { requirementId: string; label: string; status: RequirementAssessment['status']; rationale: string }[];
  uncertainty: { level: 'LOW' | 'MEDIUM' | 'HIGH'; reasons: string[] };
  requirementAssessments: RequirementAssessment[];
  contributions: { requirementId: string; weight: number; coverage: number | null; included: boolean; duplicateOf: string | null; rationale: string }[];
}
