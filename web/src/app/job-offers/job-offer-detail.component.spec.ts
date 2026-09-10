import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JobOfferDetailComponent } from './job-offer-detail.component';
import { JobOfferService } from './job-offer.service';

describe('Saved job offer', () => {
  const saved = {
    id: 'saved-id', originalText: '  Synthetic original\n', sourceUrl: null,
    reviewStatus: 'UNREVIEWED', analyzedAt: '2026-09-10T00:00:00Z',
    extraction: {
      company: 'Saved company', position: null, location: { city: null, region: null, country: null },
      workArrangement: { type: 'UNKNOWN', remoteArea: null, onSiteDaysPerWeek: null },
      contractType: 'UNKNOWN', sourceLanguage: null, summary: null, missions: [], requirements: [],
    },
  };
  let service: { get: ReturnType<typeof vi.fn> };
  beforeEach(async () => {
    service = { get: vi.fn(() => of(saved)) };
    await TestBed.configureTestingModule({
      imports: [JobOfferDetailComponent],
      providers: [provideRouter([]), { provide: JobOfferService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'saved-id' }) } } }],
    }).compileComponents();
  });
  it('reloads saved content from the API with no in-memory analysis', () => {
    const fixture = TestBed.createComponent(JobOfferDetailComponent);
    fixture.detectChanges();
    expect(service.get).toHaveBeenCalledWith('saved-id');
    expect(fixture.nativeElement.textContent).toContain('Saved company');
    expect(fixture.nativeElement.textContent).toContain('vérification recommandée');
    expect(fixture.nativeElement.querySelector('pre').textContent).toBe(saved.originalText);
    fixture.destroy();
    const reloaded = TestBed.createComponent(JobOfferDetailComponent);
    reloaded.detectChanges();
    expect(service.get).toHaveBeenCalledTimes(2);
    expect(reloaded.nativeElement.textContent).toContain('Saved company');
  });
  it('offers a retry when loading fails', () => {
    service.get.mockReturnValueOnce(throwError(() => new Error('offline')));
    const fixture = TestBed.createComponent(JobOfferDetailComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Impossible de charger');
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Saved company');
  });
});
