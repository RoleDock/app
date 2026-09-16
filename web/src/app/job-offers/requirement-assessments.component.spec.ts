import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { RequirementAssessmentsComponent } from './requirement-assessments.component';
import { JobOfferService } from './job-offer.service';
import { JobOffer } from './job-offer.models';
import { AssessmentResponse } from './requirement-assessment.models';

describe('Requirement evidence view', () => {
  const offer: JobOffer = {
    id: 'fictional-offer', originalText: 'Java', sourceUrl: null, analyzedAt: '2026-01-01', reviewStatus: 'UNREVIEWED', reviewBypassedAt: null,
    extraction: { company: null, position: null, location: { city: null, country: null, region: null },
      workArrangement: { type: 'UNKNOWN', remoteArea: null, onSiteDaysPerWeek: null }, contractType: 'UNKNOWN', sourceLanguage: null,
      summary: null, missions: [], requirements: [{ id: 'r1', source: 'USER_ADDED', rawText: null, canonicalLabel: 'Java',
        category: 'TECH_SKILL', requirementKind: 'REQUIRED', centrality: 'CORE', explicitness: 'EXPLICIT', hardBlockerCandidate: true,
        blockerCondition: 'Java', constraint: null, extractionConfidence: null }] },
  };
  const result: AssessmentResponse = { assessedOn: '2026-01-01', assessments: [{ requirementId: 'r1', status: 'UNKNOWN',
    transferRelation: 'NONE', evidenceStrength: 'WEAK', assessmentConfidence: 'LOW', eligibilityEffect: 'POSSIBLE_BLOCK',
    rationale: 'Durée inconnue.', attention: 'Vérifiez le profil.', evidence: [{ type: 'EXPERIENCE', id: 'e1', label: 'Fictional — Developer' }] }] };
  let service: { assessments: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    service = { assessments: vi.fn(() => of(result)) };
    TestBed.configureTestingModule({ imports: [RequirementAssessmentsComponent], providers: [{ provide: JobOfferService, useValue: service }] });
  });
  function render(value = offer) {
    const fixture = TestBed.createComponent(RequirementAssessmentsComponent);
    fixture.componentRef.setInput('offer', value); fixture.detectChanges(); return fixture;
  }
  it('shows backend rationale, evidence, unknown and review caution without a percentage', () => {
    const text = render().nativeElement.textContent;
    expect(text).toContain('Java'); expect(text).toContain('Requis');
    expect(text).toContain('À vérifier'); expect(text).toContain('Durée inconnue.');
    expect(text).toContain('Fictional — Developer'); expect(text).toContain('Blocage possible');
    expect(text).toContain('Analyse non vérifiée'); expect(text).not.toContain('%'); expect(text).not.toContain('Échec');
    expect(service.assessments).toHaveBeenCalledWith('fictional-offer');
  });
  it('shows loading and refreshes from the API', () => {
    const pending = new Subject<AssessmentResponse>(); service.assessments.mockReturnValueOnce(pending);
    const fixture = render(); expect(fixture.nativeElement.textContent).toContain('Évaluation en cours');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    pending.next(result); pending.complete(); fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(service.assessments).toHaveBeenCalledTimes(2);
  });
  it('preserves retry after failure and displays an empty offer', () => {
    service.assessments.mockReturnValueOnce(throwError(() => new Error('offline')));
    const fixture = render({ ...offer, extraction: { ...offer.extraction, requirements: [] } });
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible');
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune exigence à évaluer');
  });
});
