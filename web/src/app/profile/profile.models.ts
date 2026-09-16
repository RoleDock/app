export type WorkMode = 'ONSITE' | 'HYBRID' | 'REMOTE';

export interface ExperienceData {
  id: string;
  company: string | null;
  position: string | null;
  location: string | null;
  startDate: string | null;
  endDate: string | null;
  current: boolean;
  description: string | null;
  achievements: string[];
  skillIds: string[];
}

export interface SkillData {
  id: string;
  name: string;
  category: string | null;
}

export interface EducationData {
  id: string;
  institution: string | null;
  degree: string | null;
  field: string | null;
  startDate: string | null;
  endDate: string | null;
  description: string | null;
}

export interface LanguageData {
  id: string;
  name: string | null;
  level: string | null;
}

export interface CertificationData {
  id: string;
  name: string | null;
  issuer: string | null;
  issueDate: string | null;
  expirationDate: string | null;
  credentialId: string | null;
  credentialUrl: string | null;
}

export interface ProjectData {
  id: string;
  name: string | null;
  role: string | null;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
  url: string | null;
}

export interface ProfilePayload {
  mainTitle: string | null;
  targetRoles: string[];
  currentLocation: string | null;
  professionalSummary: string | null;
  mobility: string | null;
  desiredLocations: string[];
  workModes: WorkMode[];
  contractTypes: string[];
  experiences: ExperienceData[];
  skills: SkillData[];
  educations: EducationData[];
  languages: LanguageData[];
  certifications: CertificationData[];
  projects: ProjectData[];
  additionalInformation: string | null;
  certificationsComplete?: boolean;
}

export interface CandidateProfile extends ProfilePayload {
  id: string;
}
