import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { RequirementAssessmentsComponent } from './requirement-assessments.component';
import { JobOfferService } from './job-offer.service';
import { CurrentRequirement, JobOffer } from './job-offer.models';
import { OfferAnalysis } from './requirement-assessment.models';

describe('Requirement evidence view', () => {
  const offer: JobOffer = {
    id: 'fictional-offer', originalText: 'Java', sourceUrl: null, analyzedAt: '2026-01-01', reviewStatus: 'UNREVIEWED', reviewBypassedAt: null,
    extraction: { company: null, position: null, location: { city: null, country: null, region: null },
      workArrangement: { type: 'UNKNOWN', remoteArea: null, onSiteDaysPerWeek: null }, contractType: 'UNKNOWN', sourceLanguage: null,
      summary: null, missions: [], requirements: [{ id: 'r1', source: 'USER_ADDED', rawText: null, canonicalLabel: 'Java',
        category: 'TECH_SKILL', requirementKind: 'REQUIRED', centrality: 'CORE', explicitness: 'EXPLICIT', hardBlockerCandidate: true,
        blockerCondition: 'Java', constraint: null, extractionConfidence: null }] },
  };
  const result: OfferAnalysis = { assessedOn: '2026-01-01', reviewStatus: 'UNREVIEWED', coverageScore: 74, eligibility: 'ELIGIBLE_WITH_CONSTRAINT', recommendation: 'VERIFY_FIRST', criticalGaps: [{ requirementId: 'r1', label: 'Java', status: 'MISSING', rationale: 'Point critique fictif.' }], uncertainty: { level: 'HIGH', reasons: ['Condition à vérifier.'] }, contributions: [], requirementAssessments: [{ requirementId: 'r1', status: 'UNKNOWN',
    transferRelation: 'NONE', evidenceStrength: 'WEAK', assessmentConfidence: 'LOW', eligibilityEffect: 'POSSIBLE_BLOCK',
    rationale: 'Durée inconnue.', attention: 'Vérifiez le profil.', evidence: [{ type: 'EXPERIENCE', id: 'e1', label: 'Fictional — Developer' }] }] };
  let service: { analysis: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    service = { analysis: vi.fn(() => of(result)) };
    TestBed.configureTestingModule({ imports: [RequirementAssessmentsComponent], providers: [{ provide: JobOfferService, useValue: service }] });
  });
  function render(value = offer) {
    const fixture = TestBed.createComponent(RequirementAssessmentsComponent);
    fixture.componentRef.setInput('offer', value); fixture.detectChanges(); return fixture;
  }
  it('shows backend rationale, evidence, unknown and review caution with a separate explainable summary', () => {
    const text = render().nativeElement.textContent;
    expect(text).toContain('Java'); expect(text).toContain('Requis');
    expect(text).toContain('À vérifier'); expect(text).toContain('Durée inconnue.');
    expect(text).toContain('Fictional — Developer'); expect(text).toContain('Blocage possible');
    expect(text).toContain('Analyse non vérifiée'); expect(text).toContain('74 %'); expect(text).not.toContain('Échec');
    expect(service.analysis).toHaveBeenCalledWith('fictional-offer');
  });
  it('shows score meaning, critical gaps and uncertainty separately from details', () => {
    const element = render().nativeElement;
    expect(element.querySelector('.coverage').textContent).toContain('Couverture des exigences : 74 %');
    expect(element.textContent).toContain('pas vos chances d’obtenir un entretien');
    expect(element.querySelector('.critical-gaps').textContent).toContain('Point critique fictif.');
    expect(element.querySelector('.uncertainty').textContent).toContain('Condition à vérifier.');
    expect(element.querySelector('article').textContent).toContain('Durée inconnue.');
  });
  it.each([
    ['APPLY_NOW', 'Candidature pertinente'],
    ['APPLY_WITH_BRIDGE', 'Candidature pertinente avec points à expliquer'],
    ['STRETCH', 'Candidature ambitieuse'],
    ['VERIFY_FIRST', 'À vérifier avant de décider'],
    ['SKIP_CONFIRMED_BLOCKER', 'Critère bloquant confirmé'],
  ] as const)('presents recommendation %s', (recommendation, label) => {
    service.analysis.mockReturnValueOnce(of({ ...result, recommendation }));
    expect(render().nativeElement.querySelector('.recommendation').textContent).toContain(label);
  });
  it('shows a confirmed blocker even with high coverage and a reviewed API snapshot', () => {
    service.analysis.mockReturnValueOnce(of({ ...result, coverageScore: 95, reviewStatus: 'CONFIRMED', eligibility: 'NOT_ELIGIBLE', recommendation: 'SKIP_CONFIRMED_BLOCKER' }));
    const text = render().nativeElement.textContent;
    expect(text).toContain('95 %'); expect(text).toContain('Critère bloquant confirmé');
    expect(text).toContain('Points critiques'); expect(text).not.toContain('Analyse non vérifiée');
  });
  it('formats decimal coverage in French', () => {
    service.analysis.mockReturnValueOnce(of({ ...result, coverageScore: 90.91 }));
    expect(render().nativeElement.querySelector('.coverage').textContent).toContain('90,91 %');
  });
  it('shows unavailable coverage without pretending it is zero', () => {
    service.analysis.mockReturnValueOnce(of({ ...result, coverageScore: null }));
    expect(render().nativeElement.querySelector('.coverage').textContent).toContain('Indisponible');
  });
  it.each(['light', 'dark'])('preserves summary and evidence in the %s theme', theme => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      const element = render().nativeElement;
      expect(element.querySelector('.critical-gaps').textContent).toContain('Java');
      expect(element.querySelector('article').textContent).toContain('Fictional — Developer');
    } finally { document.documentElement.removeAttribute('data-theme'); }
  });
  it('shows loading and refreshes from the API', () => {
    const pending = new Subject<OfferAnalysis>(); service.analysis.mockReturnValueOnce(pending);
    const fixture = render(); expect(fixture.nativeElement.textContent).toContain('Évaluation en cours');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    pending.next(result); pending.complete(); fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(service.analysis).toHaveBeenCalledTimes(2);
  });
  it.each([
    ['OTHER', 'UNKNOWN'],
    ['UNRECOGNIZED_CATEGORY', 'UNRECOGNIZED_KIND'],
  ])('shows readable labels for category %s and kind %s', (category, requirementKind) => {
    // The second case represents a runtime API value not yet known by this client.
    const requirement = { ...offer.extraction.requirements[0],
      category: category as CurrentRequirement['category'],
      requirementKind: requirementKind as CurrentRequirement['requirementKind'] };
    const fixture = render({ ...offer, extraction: { ...offer.extraction, requirements: [requirement] } });
    expect(fixture.nativeElement.querySelector('.assessment-grid')?.previousElementSibling.textContent.trim()).toBe('Autre');
    expect(fixture.nativeElement.querySelector('article p').textContent).toContain('Non précisé');
    expect(fixture.nativeElement.querySelector('article strong').textContent).toBe('À vérifier');
  });
  it('preserves retry after failure and displays an empty offer', () => {
    service.analysis.mockReturnValueOnce(throwError(() => new Error('offline')));
    const fixture = render({ ...offer, extraction: { ...offer.extraction, requirements: [] } });
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible');
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune exigence à évaluer');
  });
});
