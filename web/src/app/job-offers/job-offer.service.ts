import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Analysis, JobOffer, ReviewCommand } from './job-offer.models';
import { AssessmentResponse, OfferAnalysis } from './requirement-assessment.models';

@Injectable({ providedIn: 'root' })
export class JobOfferService {
  private readonly http = inject(HttpClient);
  analyze(originalText: string, sourceUrl: string | null) {
    return this.http.post<Analysis>('/api/job-offers/analyze', { originalText, sourceUrl });
  }
  save(analysisId: string) {
    return this.http.post<JobOffer>('/api/job-offers', { analysisId });
  }
  review(id: string, command: ReviewCommand) {
    return this.http.put<JobOffer>(`/api/job-offers/${encodeURIComponent(id)}/review`, command);
  }
  get(id: string) {
    return this.http.get<JobOffer>(`/api/job-offers/${encodeURIComponent(id)}`);
  }
  assessments(id: string) {
    return this.http.get<AssessmentResponse>(`/api/job-offers/${encodeURIComponent(id)}/requirement-assessments`);
  }
  analysis(id: string) {
    return this.http.get<OfferAnalysis>(`/api/job-offers/${encodeURIComponent(id)}/analysis`);
  }
}
