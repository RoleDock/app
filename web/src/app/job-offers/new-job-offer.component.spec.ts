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

  it('shows loading, preserves exact text and renders structured result and review notice', () => {
    start();
    expect(text()).toContain('Analyse en cours');
    expect(service.analyze).toHaveBeenCalledWith('  Java requis\n', null);
    expect(fixture.nativeElement.querySelector('textarea').disabled).toBe(true);
    finish();
    for (const value of ['Synthetic company', 'Engineer', 'Lyon', 'Hybride', 'Contrat permanent',
      'Créer des API', 'Java requis', 'Requis', 'Central', 'Compétence technique', 'Analyse automatique — vérification recommandée']) {
      expect(text()).toContain(value);
    }
  });

  it('preserves input after extraction error and allows retry without exposing diagnostics', () => {
    start();
    analysis$.error({ message: 'provider secret', status: 502 });
    fixture.detectChanges();
    expect(text()).toContain('Votre saisie est conservée');
    expect(text()).not.toContain('provider secret');
    expect(fixture.componentInstance.form.controls.originalText.value).toBe('  Java requis\n');
    analysis$ = new Subject<Analysis>();
    service.analyze.mockReturnValue(analysis$);
    fixture.componentInstance.analyze();
    expect(service.analyze).toHaveBeenCalledTimes(2);
  });

  it('saves once while running and opens the persistent URL after success', () => {
    start(); finish();
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(service.save).toHaveBeenCalledExactlyOnceWith('analysis-id');
    expect(text()).toContain('Enregistrement…');
    save$.next({
      id: 'saved-id', originalText: '  Java requis\n', sourceUrl: null,
      analyzedAt: result.analyzedAt, reviewStatus: 'UNREVIEWED', reviewBypassedAt: null, extraction: { ...result.extraction, requirements: result.extraction.requirements.map(r => ({ ...r, id: 'requirement-id', source: 'LLM_EXTRACTED' })) },
    });
    fixture.detectChanges();
    expect(text()).toContain('Brouillon enregistré avec succès');
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/job-offers', 'saved-id']);
    fixture.componentInstance.save();
    expect(service.save).toHaveBeenCalledOnce();
  });

  it('retains text and preview after save failure and allows retry', () => {
    start(); finish();
    fixture.componentInstance.save();
    save$.error({ status: 500 });
    fixture.detectChanges();
    expect(text()).toContain('Votre saisie et le résultat sont conservés');
    expect(text()).toContain('Java requis');
    expect(fixture.componentInstance.form.controls.originalText.value).toBe('  Java requis\n');
    save$ = new Subject<JobOffer>();
    service.save.mockReturnValue(save$);
    fixture.componentInstance.save();
    expect(service.save).toHaveBeenCalledTimes(2);
  });

  it('asks for reanalysis after expiry without discarding text', () => {
    start(); finish();
    fixture.componentInstance.save();
    save$.error({ status: 409 });
    fixture.detectChanges();
    expect(text()).toContain('Relancez l’analyse');
    expect(fixture.componentInstance.analysis()).toBeNull();
    expect(fixture.componentInstance.form.controls.originalText.value).toBe('  Java requis\n');
  });

  it('invalidates preview when the original text or URL changes', () => {
    start(); finish();
    fixture.componentInstance.form.controls.sourceUrl.setValue('https://example.org');
    fixture.componentInstance.save();
    expect(service.save).not.toHaveBeenCalled();
    expect(fixture.componentInstance.analysis()).toBeNull();
  });

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
