import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

interface HealthResponse {
  status: string;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);

  getStatus(): Observable<string> {
    return this.http.get<HealthResponse>('/api/health').pipe(map((response) => response.status));
  }
}
