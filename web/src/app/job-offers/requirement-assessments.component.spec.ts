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
  const result: OfferAnalysis = { assessedOn: '2026-01-01', reviewStatus: 'UNREVIEWED', coverageScore: 74, eligibility: 'ELIGIBLE_WITH_CONSTRAINT', recommendation: 'VERIFY_FIRST', criticalGaps: [{ requirementId: 'r1', label: 'Java', status: 'MISSING', rationale: 'Point critique fictif.' }], uncertainty: { level: 'HIGH', reasons: ['Condition à vérifier.'] }, contributions: [{
    requirementId: 'r1', weight: 8, coverage: null, included: false, duplicateOf: null,
    rationale: 'Information inconnue : exclue du dénominateur.',
  }], requirementAssessments: [{ requirementId: 'r1', status: 'UNKNOWN',
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
    [0.75, '0,75'],
    [0, '0'],
    [null, 'Indisponible'],
  ] as const)('renders included contribution coverage %s with its rationale and weight', (coverage, displayed) => {
    service.analysis.mockReturnValueOnce(of({ ...result, contributions: [{
      requirementId: 'r1', weight: 0.5, coverage, included: true, duplicateOf: null,
      rationale: 'Couverture par équivalence explicite.',
    }] }));
    const article = render().nativeElement.querySelector('article');
    expect(article.textContent).toContain('Couverture par équivalence explicite.');
    expect(article.textContent).toContain('Poids : 0,5');
    expect(article.textContent).toContain(`Couverture : ${displayed} (sur 1).`);
  });
  it('explains an excluded contribution without displaying weight or coverage', () => {
    const article = render().nativeElement.querySelector('article');
    expect(article.textContent).toContain('Information inconnue : exclue du dénominateur.');
    expect(article.textContent).not.toContain('Poids :');
    expect(article.textContent).not.toContain('Couverture :');
  });
  it('associates contributions with requirement IDs rather than response order', () => {
    const second = { ...offer.extraction.requirements[0], id: 'r2', canonicalLabel: 'Angular' };
    service.analysis.mockReturnValueOnce(of({ ...result,
      requirementAssessments: [...result.requirementAssessments, { ...result.requirementAssessments[0], requirementId: 'r2' }],
      contributions: [
        { requirementId: 'r2', weight: 2, coverage: 0.5, included: true, duplicateOf: null, rationale: 'Contribution Angular.' },
        { requirementId: 'r1', weight: 8, coverage: 1, included: true, duplicateOf: null, rationale: 'Contribution Java.' },
      ],
    }));
    const articles = render({ ...offer, extraction: { ...offer.extraction,
      requirements: [offer.extraction.requirements[0], second] } }).nativeElement.querySelectorAll('article');
    expect(articles[0].textContent).toContain('Contribution Java.');
    expect(articles[0].textContent).toContain('Poids : 8');
    expect(articles[0].textContent).toContain('Couverture : 1 (sur 1).');
    expect(articles[0].textContent).not.toContain('Contribution Angular.');
    expect(articles[1].textContent).toContain('Contribution Angular.');
    expect(articles[1].textContent).toContain('Poids : 2');
    expect(articles[1].textContent).toContain('Couverture : 0,5 (sur 1).');
    expect(articles[1].textContent).not.toContain('Contribution Java.');
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
  it('uses readable fallback labels for future API enum values without losing evidence', () => {
    const unknown = 'FUTURE_VALUE';
    service.analysis.mockReturnValueOnce(of({ ...result,
      eligibility: unknown, recommendation: unknown, uncertainty: { level: unknown, reasons: ['À confirmer.'] },
      criticalGaps: [{ ...result.criticalGaps[0], status: unknown }],
      requirementAssessments: [{ ...result.requirementAssessments[0], status: unknown, transferRelation: unknown,
        evidenceStrength: unknown, assessmentConfidence: unknown,
        evidence: [{ type: unknown, id: 'e1', label: 'Preuve fictive conservée' }] }],
    }));
    const element = render().nativeElement;
    expect(element.querySelector('.analysis-summary').textContent).toContain('Éligibilité : Indéterminée');
    expect(element.querySelector('.recommendation').textContent).toContain('À vérifier avant de décider');
    expect(element.querySelector('.uncertainty').textContent).toContain('Incertitude : non déterminée');
    expect(element.querySelector('.critical-gaps').textContent).toContain('À vérifier');
    expect(element.querySelector('article').textContent).toContain('Relation à vérifier');
    expect(element.querySelector('article').textContent).toContain('Preuve : non précisée');
    expect(element.querySelector('article').textContent).toContain('Confiance : non précisée');
    expect(element.querySelector('article').textContent).toContain('Élément du profil : Preuve fictive conservée');
    expect(element.textContent).not.toContain(unknown);
  });
  it('renders an unavailable score when the runtime response omits it', () => {
    service.analysis.mockReturnValueOnce(of({ ...result, coverageScore: undefined }));
    expect(render().nativeElement.querySelector('.coverage').textContent).toContain('Indisponible');
  });
  it('preserves retry after failure and displays an empty offer', () => {
    service.analysis.mockReturnValueOnce(throwError(() => new Error('offline')));
    const fixture = render({ ...offer, extraction: { ...offer.extraction, requirements: [] } });
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible');
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune exigence à évaluer');
  });
});
