import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CandidateProfile, ProfilePayload } from './profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  getCurrent(): Observable<CandidateProfile | null> {
    return this.http.get<CandidateProfile | null>('/api/profile');
  }

  save(profile: ProfilePayload): Observable<CandidateProfile> {
    return this.http.put<CandidateProfile>('/api/profile', profile);
  }
}
