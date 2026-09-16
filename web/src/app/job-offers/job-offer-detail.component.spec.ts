import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JobOfferDetailComponent } from './job-offer-detail.component';
import { JobOfferService } from './job-offer.service';

describe('Saved job offer', () => {
  const saved = {
    id: 'saved-id', originalText: '  Synthetic original\n', sourceUrl: null,
    reviewStatus: 'UNREVIEWED', reviewBypassedAt: null, analyzedAt: '2026-09-10T00:00:00Z',
    extraction: {
      company: 'Saved company', position: null, location: { city: null, region: null, country: null },
      workArrangement: { type: 'UNKNOWN', remoteArea: null, onSiteDaysPerWeek: null },
      contractType: 'UNKNOWN', sourceLanguage: null, summary: null, missions: [], requirements: [],
    },
  };
  let service: { get: ReturnType<typeof vi.fn>; assessments: ReturnType<typeof vi.fn> };
  beforeEach(async () => {
    service = { get: vi.fn(() => of(saved)), assessments: vi.fn(() => of({ assessedOn: '2026-01-01', assessments: [] })) };
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
  it.each([
    ['UNREVIEWED', '2026-09-10T01:00:00Z', 'Analyse automatique — non vérifiée'],
    ['CONFIRMED', '2026-09-10T01:00:00Z', 'Analyse vérifiée'],
    ['CORRECTED', '2026-09-10T01:00:00Z', 'Analyse vérifiée et corrigée'],
  ])('shows persisted %s state even after bypass', (reviewStatus, reviewBypassedAt, label) => {
    service.get.mockReturnValue(of({ ...saved, reviewStatus, reviewBypassedAt }));
    const fixture = TestBed.createComponent(JobOfferDetailComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toBe(label);
    expect(fixture.nativeElement.textContent).toContain('Vérifier ou corriger');
  });
  it('groups requirements into independently expandable categories', () => {
    const requirement = { canonicalLabel: 'Java', rawText: 'Java requis', category: 'TECH_SKILL', requirementKind: 'REQUIRED', centrality: 'CORE' };
    service.get.mockReturnValue(of({ ...saved, extraction: { ...saved.extraction, requirements: [requirement, { ...requirement, canonicalLabel: 'Spring' }, { ...requirement, category: 'EXPERIENCE' }] } }));
    const fixture = TestBed.createComponent(JobOfferDetailComponent);
    fixture.detectChanges();
    const toggles = fixture.nativeElement.querySelectorAll('.category-toggle') as NodeListOf<HTMLButtonElement>;
    const contents = fixture.nativeElement.querySelectorAll('.category-content') as NodeListOf<HTMLElement>;
    expect(toggles).toHaveLength(2);
    expect(toggles[0].textContent).toContain('Compétence technique');
    expect(toggles[0].textContent).toContain('2');
    expect(contents[0].hidden).toBe(true);
    toggles[0].click(); fixture.detectChanges();
    expect(contents[0].hidden).toBe(false);
    expect(contents[1].hidden).toBe(true);
    expect(contents[0].querySelectorAll('article')).toHaveLength(2);
    toggles[0].click(); fixture.detectChanges();
    expect(contents[0].hidden).toBe(true);
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
