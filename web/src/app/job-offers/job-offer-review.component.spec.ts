import { ComponentFixture } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { JobOfferReviewComponent } from './job-offer-review.component';
import { JobOfferService } from './job-offer.service';
import { JobOffer } from './job-offer.models';

describe('Job offer review', () => {
  let fixture: ComponentFixture<JobOfferReviewComponent>;
  let result: Subject<JobOffer>;
  let service: { get: ReturnType<typeof vi.fn>; review: ReturnType<typeof vi.fn> };
  const saved: JobOffer = {
    id: 'saved-id', originalText: '  Synthetic original advertisement\nJava required.  ', sourceUrl: null,
    analyzedAt: '2026-09-10T00:00:00Z', reviewStatus: 'UNREVIEWED', reviewBypassedAt: null,
    extraction: { company: 'Synthetic company', position: 'Engineer', location: { city: null, region: null, country: null },
      workArrangement: { type: 'UNKNOWN', remoteArea: null, onSiteDaysPerWeek: null }, contractType: 'UNKNOWN',
      sourceLanguage: null, summary: null, missions: ['Build APIs'], requirements: [{
        id: 'requirement-id', source: 'LLM_EXTRACTED', rawText: 'Java required.', canonicalLabel: 'Java',
        category: 'TECH_SKILL', requirementKind: 'REQUIRED', centrality: 'CORE', explicitness: 'EXPLICIT',
        hardBlockerCandidate: false, blockerCondition: null, constraint: null, extractionConfidence: 'HIGH',
      }] },
  };
  beforeEach(async () => {
    result = new Subject<JobOffer>();
    service = { get: vi.fn(() => of(structuredClone(saved))), review: vi.fn(() => result) };
    await TestBed.configureTestingModule({ imports: [JobOfferReviewComponent], providers: [provideRouter([]),
      { provide: JobOfferService, useValue: service },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: saved.id }) } } },
    ] }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(JobOfferReviewComponent);
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
  });
  async function input(name: string, value: string) {
    const element = fixture.nativeElement.querySelector(`[name="${name}"]`) as HTMLInputElement;
    element.value = value; element.dispatchEvent(new Event('input'));
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
  }
  function button(text: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('button')].find(b => (b as HTMLButtonElement).textContent?.includes(text)) as HTMLButtonElement;
  }
  it('toggles categories without losing field values or validation', async () => {
    const toggle = fixture.nativeElement.querySelector('.category-toggle') as HTMLButtonElement;
    const content = fixture.nativeElement.querySelector('.category-content') as HTMLElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(content.hidden).toBe(true);
    toggle.click(); fixture.detectChanges();
    await input('canonicalLabel', '');
    toggle.click(); fixture.detectChanges();
    expect(content.hidden).toBe(true);
    expect((fixture.nativeElement.querySelector('#review-save') as HTMLButtonElement).disabled).toBe(true);
    expect(fixture.componentInstance.draft!.requirements[0].canonicalLabel).toBe('');
  });

  it('opens a category on navigation and moves edits into their new category', async () => {
    fixture.componentInstance.goToRequirement(0);
    fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.category-content').hidden).toBe(false);
    const select = fixture.nativeElement.querySelector('[name="category"]') as HTMLSelectElement;
    select.value = 'EXPERIENCE'; select.dispatchEvent(new Event('change'));
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.category-toggle').textContent).toContain('Expérience');
    expect(fixture.nativeElement.querySelector('.category-content').hidden).toBe(false);
    await input('canonicalLabel', 'Expérience Java');
    fixture.componentInstance.save();
    expect(service.review.mock.calls[0][1].extraction.requirements[0]).toMatchObject({ category: 'EXPERIENCE', canonicalLabel: 'Expérience Java', rawText: 'Java required.' });
  });

  it('follows grouped display order and opens the next category', async () => {
    const component = fixture.componentInstance;
    const first = component.draft!.requirements[0];
    component.draft!.requirements.push({ ...first, id: 'experience', category: 'EXPERIENCE' }, { ...first, id: 'second-skill' });
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges(); await fixture.whenStable();
    component.completeRequirement(first, 0);
    fixture.detectChanges(); await fixture.whenStable();
    expect(component.activeRequirement()).toBe(2);
    component.completeRequirement(component.draft!.requirements[2], 2);
    fixture.detectChanges(); await fixture.whenStable();
    expect(component.activeRequirement()).toBe(1);
    expect(component.openCategories.has('EXPERIENCE')).toBe(true);
    button('Ajouter une exigence').click();
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(component.openCategories.has('OTHER')).toBe(true);
    expect((fixture.nativeElement.querySelector('#review-save') as HTMLButtonElement).disabled).toBe(true);
  });

  it('shows the intact original, citation, status and a calm bypass warning', () => {
    expect(fixture.nativeElement.querySelector('pre').textContent).toBe(saved.originalText);
    expect(fixture.nativeElement.querySelector('blockquote').textContent).toBe('Java required.');
    expect(fixture.nativeElement.textContent).toContain('vérification recommandée');
    expect(fixture.nativeElement.textContent).toContain('peut contenir des erreurs');
    expect(fixture.nativeElement.querySelector('[name="rawText"]')).toBeNull();
  });
  it('edits a field and requirement and saves all corrections without changing the quotation', async () => {
    await input('company', 'Corrected synthetic company');
    await input('canonicalLabel', 'Java platform');
    expect(fixture.nativeElement.textContent).toContain('Modifications en attente');
    expect(button('Continuer sans vérifier').disabled).toBe(true);
    button('Valider les corrections').click();
    const command = service.review.mock.calls[0][1];
    expect(command.extraction.company).toBe('Corrected synthetic company');
    expect(command.extraction.requirements[0].canonicalLabel).toBe('Java platform');
    expect(command.extraction.requirements[0].rawText).toBe('Java required.');
    result.next({ ...saved, extraction: command.extraction, reviewStatus: 'CORRECTED' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Analyse vérifiée et corrigée');
    expect(fixture.componentInstance.dirty()).toBe(false);
  });
  it('confirms unchanged analysis and shows the reviewed badge', () => {
    button('Valider l’offre').click();
    expect(service.review).toHaveBeenCalledWith(saved.id, { action: 'SAVE', extraction: saved.extraction });
    result.next({ ...saved, reviewStatus: 'CONFIRMED' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Analyse vérifiée');
    expect(button('Continuer sans vérifier')).toBeUndefined();
  });
  it('adds and removes requirements with explicit manual provenance', async () => {
    button('Ajouter une exigence').click(); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Ajoutée par vous');
    expect(fixture.componentInstance.draft!.requirements[1].rawText).toBeNull();
    button('Supprimer l’exigence 1').click(); fixture.detectChanges();
    expect(fixture.componentInstance.draft!.requirements).toHaveLength(1);
    expect(fixture.componentInstance.draft!.requirements[0].source).toBe('USER_ADDED');
  });
  it('edits, adds and removes missions in order', async () => {
    await input('mission-0', 'Corrected mission');
    button('Ajouter une mission').click(); fixture.detectChanges(); await fixture.whenStable();
    await input('mission-1', 'Second mission');
    button('Supprimer la mission 1').click(); fixture.detectChanges();
    expect(fixture.componentInstance.draft!.missions).toEqual(['Second mission']);
  });
  it('bypasses without changing review status and continues to the saved offer', () => {
    button('Continuer sans vérifier').click();
    expect(service.review).toHaveBeenCalledWith(saved.id, { action: 'BYPASS' });
    result.next({ ...saved, reviewBypassedAt: '2026-09-10T01:00:00Z' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Analyse automatique — non vérifiée');
    expect(fixture.nativeElement.textContent).not.toContain('Offre validée');
    expect(fixture.nativeElement.querySelector('.notice.success')).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/job-offers', saved.id]);
  });
  it.each([false, 'reject'])('retries bypass navigation (%s) without another review write', async outcome => {
    const navigate = vi.mocked(TestBed.inject(Router).navigate);
    if (outcome === 'reject') navigate.mockRejectedValueOnce(new Error('navigation failed'));
    else navigate.mockResolvedValueOnce(false);
    button('Continuer sans vérifier').click();
    result.next({ ...saved, reviewBypassedAt: '2026-09-10T01:00:00Z' }); result.complete();
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Réessayez pour ouvrir l’offre');
    expect(fixture.nativeElement.textContent).not.toContain('Offre validée');
    button('Continuer sans vérifier').click();
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledTimes(2);
    expect(service.review).toHaveBeenCalledOnce();
  });
  it.each(['CONFIRMED', 'CORRECTED'] as const)('reviews a reloaded bypassed offer as %s', async reviewStatus => {
    const bypassed = { ...structuredClone(saved), reviewBypassedAt: '2026-09-10T01:00:00Z' };
    service.get.mockReturnValue(of(bypassed));
    fixture.componentInstance.load(); fixture.detectChanges(); await fixture.whenStable();
    if (reviewStatus === 'CORRECTED') await input('company', 'Later correction');
    button(reviewStatus === 'CORRECTED' ? 'Valider les corrections' : 'Valider l’offre').click();
    const command = service.review.mock.calls[0][1];
    expect(command.action).toBe('SAVE');
    result.next({ ...bypassed, extraction: command.extraction, reviewStatus }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Analyse vérifiée');
    expect(fixture.nativeElement.textContent).not.toContain('Analyse automatique — non vérifiée');
  });
  it('preserves unsaved edits on API error and allows retry', async () => {
    await input('company', 'Keep my edits');
    button('Valider les corrections').click();
    result.error({ error: { fieldErrors: { company: 'Synthetic validation failure' } } }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[name="company"]').value).toBe('Keep my edits');
    expect(fixture.nativeElement.textContent).toContain('Synthetic validation failure');
    expect(fixture.componentInstance.dirty()).toBe(true);
    expect(button('Valider les corrections').disabled).toBe(false);
  });
  it('warns before leaving with unsaved changes', async () => {
    await input('company', 'Unsaved');
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    expect(fixture.componentInstance.canLeave()).toBe(false);
    const event = new Event('beforeunload', { cancelable: true });
    fixture.componentInstance.beforeUnload(event as BeforeUnloadEvent);
    expect(event.defaultPrevented).toBe(true);
  });
  it('keeps save available in the toolbar and jumps directly to a requirement', async () => {
    const jump = fixture.nativeElement.querySelector('select[aria-label="Aller à une exigence"]') as HTMLSelectElement;
    jump.selectedIndex = 1; jump.dispatchEvent(new Event('change'));
    fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.componentInstance.activeRequirement()).toBe(0);
    expect(fixture.nativeElement.querySelector('.review-toolbar button[type="submit"]')).not.toBeNull();
  });
  it('checks and collapses a requirement, advances, and preserves edits and form validation', async () => {
    button('Ajouter une exigence').click(); fixture.detectChanges(); await fixture.whenStable();
    await input('canonicalLabel', 'Corrected Java');
    button('Vérifier et passer à la suivante').click(); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#requirement-content-0').hidden).toBe(true);
    expect(fixture.componentInstance.activeRequirement()).toBe(1);
    expect(fixture.componentInstance.checked.size).toBe(1);
    expect(service.review).not.toHaveBeenCalled();
    expect(button('Valider les corrections').disabled).toBe(true);
    fixture.componentInstance.goToRequirement(0); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#requirement-content-0').hidden).toBe(false);
    expect(fixture.nativeElement.querySelector('[name="canonicalLabel"]').value).toBe('Corrected Java');
  });

  it('jumps to the last of 30 requirements without scrolling through them', async () => {
    const many = structuredClone(saved);
    many.extraction.requirements = Array.from({ length: 30 }, (_, i) => ({
      ...saved.extraction.requirements[0], id: `requirement-${i}`, canonicalLabel: `Requirement ${i + 1}`,
    }));
    service.get.mockReturnValue(of(many));
    fixture.componentInstance.load(); fixture.detectChanges(); await fixture.whenStable();
    const jump = fixture.nativeElement.querySelector('select[aria-label="Aller à une exigence"]') as HTMLSelectElement;
    jump.selectedIndex = 30; jump.dispatchEvent(new Event('change'));
    fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.componentInstance.activeRequirement()).toBe(29);
    expect(fixture.nativeElement.querySelector('#requirement-content-29').hidden).toBe(false);
    expect(button('Valider l’offre').disabled).toBe(false);
  });

});
