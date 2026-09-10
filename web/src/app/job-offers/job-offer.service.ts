import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Analysis, JobOffer } from './job-offer.models';

@Injectable({ providedIn: 'root' })
export class JobOfferService {
  private readonly http = inject(HttpClient);
  analyze(originalText: string, sourceUrl: string | null) {
    return this.http.post<Analysis>('/api/job-offers/analyze', { originalText, sourceUrl });
  }
  save(analysisId: string) {
    return this.http.post<JobOffer>('/api/job-offers', { analysisId });
  }
  get(id: string) {
    return this.http.get<JobOffer>(`/api/job-offers/${encodeURIComponent(id)}`);
  }
}
