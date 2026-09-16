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
