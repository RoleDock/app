import { ComponentFixture, TestBed } from '@angular/core/testing';
import { JobOfferProgressComponent } from './job-offer-progress.component';

describe('Job offer progress (without services)', () => {
  let fixture: ComponentFixture<JobOfferProgressComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [JobOfferProgressComponent] }).compileComponents();
    fixture = TestBed.createComponent(JobOfferProgressComponent);
    fixture.componentRef.setInput('page', 'new');
  });
  function render(inputs: Record<string, unknown> = {}) {
    for (const [key, value] of Object.entries(inputs)) fixture.componentRef.setInput(key, value);
    fixture.detectChanges();
    return [...fixture.nativeElement.querySelectorAll('li')] as HTMLElement[];
  }
  function current() { return fixture.nativeElement.querySelector('[aria-current="step"]')?.textContent; }

  it('starts on Offre with four readable, non-clickable stages', () => {
    expect(render()).toHaveLength(4);
    expect(current()).toContain('Offre');
    expect(fixture.nativeElement.querySelectorAll('a, button')).toHaveLength(0);
  });
  it('completes input and announces processing without a percentage', () => {
    const steps = render({ analysisState: 'processing' });
    expect(steps[0].textContent).toContain('Terminée');
    expect(current()).toContain('Analyse');
    expect(steps[1].textContent).toContain('En cours');
    expect(steps[3].textContent).toContain('À suivre');
    expect(fixture.nativeElement.textContent).not.toContain('%');
  });
  it('keeps input complete after failure and supports retry', () => {
    const steps = render({ analysisState: 'error' });
    expect(steps[0].textContent).toContain('Terminée');
    expect(current()).toContain('À réessayer');
    render({ analysisState: 'processing' });
    expect(current()).toContain('En cours');
  });
  it('completes analysis and makes optional verification current on review', () => {
    const steps = render({ page: 'review', reviewStatus: 'UNREVIEWED' });
    expect(steps[1].textContent).toContain('Terminée');
    expect(current()).toContain('Vérification');
    expect(current()).toContain('facultative');
  });
  it.each(['CONFIRMED', 'CORRECTED'])('completes verification for %s even with a historic bypass', reviewStatus => {
    const steps = render({ page: 'review', reviewStatus, reviewBypassedAt: '2026-09-15' });
    expect(steps[2].classList.contains('completed')).toBe(true);
    expect(steps[2].textContent).toContain(reviewStatus === 'CORRECTED' ? 'Vérifiée et corrigée' : 'Vérifiée');
    expect(current()).toContain('Offre prête');
  });
  it.each(['review', 'detail'])('reaches ready after bypass on %s without confirming or showing failure', page => {
    const steps = render({ page, reviewStatus: 'UNREVIEWED', reviewBypassedAt: '2026-09-15' });
    expect(steps[2].textContent).toContain('Ignorée · non vérifiée');
    expect(steps[2].classList.contains('bypassed')).toBe(true);
    expect(steps[2].classList.contains('error')).toBe(false);
    expect(steps[2].classList.contains('completed')).toBe(false);
    expect(current()).toContain('Offre prête');
    expect(fixture.componentInstance.reviewStatus()).toBe('UNREVIEWED');
  });
  it('does not invent bypass when an unreviewed saved offer is opened directly', () => {
    const steps = render({ page: 'detail', reviewStatus: 'UNREVIEWED' });
    expect(current()).toContain('Offre prête');
    expect(steps[2].classList.contains('pending')).toBe(true);
    expect(steps[2].textContent).not.toContain('Ignorée');
  });
  it('keeps verification current while saved content has unsaved edits', () => {
    render({ page: 'review', reviewStatus: 'CONFIRMED', editing: true });
    expect(current()).toContain('Vérification');
  });
  it('does not claim completion before the saved offer has loaded', () => {
    const steps = render({ page: 'detail' });
    expect(steps.every(step => step.classList.contains('pending'))).toBe(true);
    expect(current()).toBeUndefined();
  });
  it('keeps analysis complete when only navigation to review remains', () => {
    const steps = render({ analysisState: 'completed' });
    expect(steps[1].textContent).toContain('Terminée');
    expect(current()).toContain('Vérification');
  });
});
