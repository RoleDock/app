import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { NewJobOfferComponent } from './new-job-offer.component';
import { JobOfferService } from './job-offer.service';
import { Analysis, JobOffer } from './job-offer.models';

describe('New job offer', () => {
  let fixture: ComponentFixture<NewJobOfferComponent>;
  let analysis$: Subject<Analysis>;
  let save$: Subject<JobOffer>;
  let service: { analyze: ReturnType<typeof vi.fn>; save: ReturnType<typeof vi.fn> };
  const result: Analysis = {
    analysisId: 'analysis-id', analyzedAt: '2026-09-10T00:00:00Z',
    extraction: {
      company: 'Synthetic company', position: 'Engineer', location: { city: 'Lyon', region: null, country: null },
      workArrangement: { type: 'HYBRID', remoteArea: 'France', onSiteDaysPerWeek: 2 },
      contractType: 'PERMANENT', sourceLanguage: 'fr', summary: 'Une annonce synthétique.',
      missions: ['Créer des API'], requirements: [{
        rawText: 'Java requis', canonicalLabel: 'Java', category: 'TECH_SKILL', requirementKind: 'REQUIRED',
        centrality: 'CORE', explicitness: 'EXPLICIT', hardBlockerCandidate: false,
        blockerCondition: null, constraint: null, extractionConfidence: 'HIGH',
      }],
    },
  };

  beforeEach(async () => {
    analysis$ = new Subject<Analysis>();
    save$ = new Subject<JobOffer>();
    service = { analyze: vi.fn(() => analysis$), save: vi.fn(() => save$) };
    await TestBed.configureTestingModule({
      imports: [NewJobOfferComponent],
      providers: [provideRouter([]), { provide: JobOfferService, useValue: service }],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(NewJobOfferComponent);
    fixture.detectChanges();
  });

  it('rejects empty and whitespace-only advertisements', () => {
    fixture.componentInstance.analyze();
    fixture.componentInstance.form.controls.originalText.setValue('   ');
    fixture.componentInstance.analyze();
    fixture.detectChanges();
    expect(service.analyze).not.toHaveBeenCalled();
    expect(text()).toContain('annonce non vide');
  });

  it('analyzes, automatically saves once and opens correction without an intermediate step', async () => {
    start();
    expect(text()).toContain('Analyse en cours');
    expect(service.analyze).toHaveBeenCalledWith('  Java requis\n', null);
    fixture.componentInstance.analyze();
    expect(service.analyze).toHaveBeenCalledOnce();
    finish();
    expect(service.save).toHaveBeenCalledExactlyOnceWith('analysis-id');
    expect(text()).toContain('Ouverture de la correction');
    expect(text()).not.toContain('brouillon');
    expect(fixture.nativeElement.querySelector('textarea').disabled).toBe(true);
    fixture.componentInstance.analyze();
    expect(service.save).toHaveBeenCalledOnce();
    saved();
    await fixture.whenStable();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/job-offers', 'saved-id', 'review']);
  });

  it('preserves input after extraction error and allows retry without exposing diagnostics', () => {
    start();
    analysis$.error({ message: 'provider secret', status: 502 });
    fixture.detectChanges();
    expect(text()).toContain('Votre saisie est conservée');
    expect(text()).not.toContain('provider secret');
    expect(service.save).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.originalText.value).toBe('  Java requis\n');
    analysis$ = new Subject<Analysis>();
    service.analyze.mockReturnValue(analysis$);
    fixture.componentInstance.analyze();
    expect(service.analyze).toHaveBeenCalledTimes(2);
  });

  it('retries persistence with the existing analysis after a failure', () => {
    start(); finish();
    save$.error({ status: 500 });
    fixture.detectChanges();
    expect(text()).toContain('Votre saisie et le résultat sont conservés');
    expect(fixture.nativeElement.querySelector('textarea').disabled).toBe(false);
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    save$ = new Subject<JobOffer>();
    service.save.mockReturnValue(save$);
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    expect(service.save).toHaveBeenCalledTimes(2);
    expect(service.analyze).toHaveBeenCalledOnce();
  });

  it('reanalyzes after expiry without discarding the source text', () => {
    start(); finish();
    save$.error({ status: 409 });
    fixture.detectChanges();
    expect(text()).toContain('Relancez l’analyse');
    expect(fixture.componentInstance.analysis()).toBeNull();
    expect(fixture.componentInstance.form.controls.originalText.value).toBe('  Java requis\n');
    analysis$ = new Subject<Analysis>();
    service.analyze.mockReturnValue(analysis$);
    fixture.componentInstance.analyze();
    expect(service.analyze).toHaveBeenCalledTimes(2);
  });

  it.each(['originalText', 'sourceUrl'] as const)('discards stale analysis when %s changes after a failure', (field) => {
    start(); finish();
    save$.error({ status: 500 });
    fixture.componentInstance.form.controls[field].setValue('https://example.org');
    expect(fixture.componentInstance.analysis()).toBeNull();
    analysis$ = new Subject<Analysis>();
    service.analyze.mockReturnValue(analysis$);
    fixture.componentInstance.analyze();
    expect(service.analyze).toHaveBeenCalledTimes(2);
    expect(service.save).toHaveBeenCalledOnce();
  });

  it.each([false, 'reject'])('retries failed navigation (%s) without saving another offer', async (outcome) => {
    const navigate = vi.mocked(TestBed.inject(Router).navigate);
    if (outcome === 'reject') navigate.mockRejectedValueOnce(new Error('navigation failed'));
    else navigate.mockResolvedValueOnce(false);
    start(); finish(); saved();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(text()).toContain('Réessayez pour ouvrir la correction');
    fixture.componentInstance.analyze();
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledTimes(2);
    expect(service.save).toHaveBeenCalledOnce();
    expect(service.analyze).toHaveBeenCalledOnce();
  });

  function saved() {
    save$.next({
      id: 'saved-id', originalText: '  Java requis\n', sourceUrl: null,
      analyzedAt: result.analyzedAt, reviewStatus: 'UNREVIEWED', reviewBypassedAt: null,
      extraction: { ...result.extraction, requirements: result.extraction.requirements.map(r => ({ ...r, id: 'requirement-id', source: 'LLM_EXTRACTED' })) },
    });
    save$.complete();
  }

  function text(): string { return fixture.nativeElement.textContent; }
  function start() {
    fixture.componentInstance.form.controls.originalText.setValue('  Java requis\n');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }
  function finish() {
    analysis$.next(result);
    analysis$.complete();
    fixture.detectChanges();
  }
});
