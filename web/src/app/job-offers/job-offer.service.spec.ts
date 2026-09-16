import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { JobOfferService } from './job-offer.service';
import { AssessmentResponse } from './requirement-assessment.models';

describe('JobOfferService', () => {
  let service: JobOfferService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(JobOfferService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('sends the original text intact for analysis', () => {
    service.analyze('  Synthetic\r\n', null).subscribe();
    const req = http.expectOne('/api/job-offers/analyze');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ originalText: '  Synthetic\r\n', sourceUrl: null });
    req.flush({});
  });
  it('saves only the server analysis identifier', () => {
    service.save('analysis-id').subscribe();
    const req = http.expectOne('/api/job-offers');
    expect(req.request.body).toEqual({ analysisId: 'analysis-id' });
    req.flush({});
  });
  it('sends an explicit review command', () => {
    service.review('saved-id', { action: 'BYPASS' }).subscribe();
    const req = http.expectOne('/api/job-offers/saved-id/review');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ action: 'BYPASS' });
    req.flush({});
  });
  it('retrieves the persisted offer by URL identifier', () => {
    service.get('saved-id').subscribe();
    const req = http.expectOne('/api/job-offers/saved-id');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
  it('retrieves offer analysis with an encoded identifier', () => {
    service.analysis('offer/id ?#').subscribe();
    const req = http.expectOne('/api/job-offers/offer%2Fid%20%3F%23/analysis');
    expect(req.request.method).toBe('GET'); req.flush({});
  });
  it('retrieves assessments with GET and an encoded offer identifier', () => {
    const response: AssessmentResponse = { assessedOn: '2026-09-16', assessments: [] };
    const received = vi.fn();
    service.assessments('offer/id ?#').subscribe(received);
    const req = http.expectOne('/api/job-offers/offer%2Fid%20%3F%23/requirement-assessments');
    expect(req.request.method).toBe('GET');
    req.flush(response);
    expect(received).toHaveBeenCalledExactlyOnceWith(response);
  });
});
