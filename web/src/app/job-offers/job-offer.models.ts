export interface Extraction {
  company: string | null;
  position: string | null;
  location: { city: string | null; region: string | null; country: string | null };
  workArrangement: {
    type: 'ONSITE' | 'HYBRID' | 'REMOTE' | 'UNKNOWN';
    remoteArea: string | null;
    onSiteDaysPerWeek: number | null;
  };
  contractType: 'PERMANENT' | 'FIXED_TERM' | 'FREELANCE' | 'INTERNSHIP' | 'APPRENTICESHIP' | 'TEMPORARY' | 'OTHER' | 'UNKNOWN';
  sourceLanguage: string | null;
  summary: string | null;
  missions: string[];
  requirements: Requirement[];
}
export interface Requirement {
  rawText: string;
  canonicalLabel: string;
  category: 'TECH_SKILL' | 'EXPERIENCE' | 'DOMAIN_KNOWLEDGE' | 'TITLE_LEVEL' | 'EDUCATION' | 'CERTIFICATION' | 'LOCATION' | 'WORK_AUTHORIZATION' | 'LANGUAGE' | 'AVAILABILITY' | 'CONTRACT' | 'SOFT_SKILL' | 'OTHER';
  requirementKind: 'REQUIRED' | 'PREFERRED' | 'CONTEXTUAL' | 'UNKNOWN';
  centrality: 'CORE' | 'SUPPORTING' | 'INCIDENTAL' | 'UNKNOWN';
  explicitness: 'EXPLICIT' | 'INFERRED';
  hardBlockerCandidate: boolean;
  blockerCondition: string | null;
  constraint: { operator: 'AT_LEAST' | 'AT_MOST' | 'EQUALS' | 'RANGE' | 'OTHER'; value: string; unit: string | null } | null;
  extractionConfidence: 'HIGH' | 'MEDIUM' | 'LOW';
}
export interface Analysis {
  analysisId: string;
  analyzedAt: string;
  extraction: Extraction;
}
export interface JobOffer {
  id: string;
  originalText: string;
  sourceUrl: string | null;
  analyzedAt: string;
  reviewStatus: 'UNREVIEWED' | 'CONFIRMED' | 'CORRECTED';
  extraction: Extraction;
}
