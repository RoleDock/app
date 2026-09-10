import { Component, DestroyRef, ElementRef, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrentExtraction, CurrentRequirement, JobOffer, ReviewCommand, reviewLabel } from './job-offer.models';
import { JobOfferService } from './job-offer.service';
import { labels } from './job-offer-preview.component';

@Component({
  selector: 'app-job-offer-review',
  imports: [FormsModule, RouterLink],
  templateUrl: './job-offer-review.component.html',
  styleUrl: './job-offer-review.component.scss',
})
export class JobOfferReviewComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly collapsed = new Set<CurrentRequirement>();
  readonly checked = new Set<CurrentRequirement>();
  readonly activeRequirement = signal(-1);
  goToSection(id: string): void {
    const element = this.host.nativeElement.querySelector<HTMLElement>('#' + id);
    element?.focus({ preventScroll: true });
    element?.scrollIntoView?.({ block: 'start' });
  }
  goToRequirement(index: number): void {
    const requirement = this.draft?.requirements[index];
    if (!requirement) return;
    this.activeRequirement.set(index);
    this.collapsed.delete(requirement);
    setTimeout(() => this.goToSection('requirement-' + index));
  }
  completeRequirement(requirement: CurrentRequirement, index: number): void {
    this.checked.add(requirement);
    this.collapsed.add(requirement);
    if (index + 1 < (this.draft?.requirements.length ?? 0)) this.goToRequirement(index + 1);
    else this.goToSection('review-save');
  }
  removeRequirement(index: number): void {
    const requirement = this.draft?.requirements[index];
    if (requirement) { this.checked.delete(requirement); this.collapsed.delete(requirement); }
    this.draft?.requirements.splice(index, 1);
    this.activeRequirement.set(-1);
  }
  toggleRequirement(requirement: CurrentRequirement): void {
    if (this.collapsed.has(requirement)) this.collapsed.delete(requirement);
    else this.collapsed.add(requirement);
  }
  private readonly service = inject(JobOfferService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly offer = signal<JobOffer | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly fieldErrors = signal<string[]>([]);
  readonly success = signal(false);
  readonly reviewLabel = reviewLabel;
  draft: CurrentExtraction | null = null;
  private baseline = '';
  readonly contracts = ['PERMANENT', 'FIXED_TERM', 'FREELANCE', 'INTERNSHIP', 'APPRENTICESHIP', 'TEMPORARY', 'OTHER', 'UNKNOWN'];
  readonly arrangements = ['ONSITE', 'HYBRID', 'REMOTE', 'UNKNOWN'];
  readonly categories = ['TECH_SKILL', 'EXPERIENCE', 'DOMAIN_KNOWLEDGE', 'TITLE_LEVEL', 'EDUCATION', 'CERTIFICATION', 'LOCATION', 'WORK_AUTHORIZATION', 'LANGUAGE', 'AVAILABILITY', 'CONTRACT', 'SOFT_SKILL', 'OTHER'];
  readonly kinds = ['REQUIRED', 'PREFERRED', 'CONTEXTUAL', 'UNKNOWN'];
  readonly centralities = ['CORE', 'SUPPORTING', 'INCIDENTAL', 'UNKNOWN'];
  readonly operators = ['AT_LEAST', 'AT_MOST', 'EQUALS', 'RANGE', 'OTHER'];
  label(value: string): string { return labels[value] ?? value; }
  constructor() { this.load(); }
  dirty(): boolean { return this.draft !== null && JSON.stringify(this.draft) !== this.baseline; }
  canLeave(): boolean { return !this.saving() && (!this.dirty() || window.confirm('Des modifications ne sont pas enregistrées. Quitter cette page ?')); }
  @HostListener('window:beforeunload', ['$event'])
  beforeUnload(event: BeforeUnloadEvent): void { if (this.dirty() || this.saving()) event.preventDefault(); }
  load(): void {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.service.get(this.route.snapshot.paramMap.get('id') ?? '').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: offer => { this.accept(offer); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger cette offre. Réessayez.'); },
    });
  }
  private accept(offer: JobOffer): void {
    this.collapsed.clear(); this.checked.clear(); this.activeRequirement.set(-1);
    this.offer.set(offer);
    this.draft = structuredClone(offer.extraction);
    this.baseline = JSON.stringify(this.draft);
  }
  addRequirement(): void {
    this.draft?.requirements.push({ id: null, source: 'USER_ADDED', rawText: null,
      canonicalLabel: '', category: 'OTHER', requirementKind: 'UNKNOWN', centrality: 'UNKNOWN',
      explicitness: 'EXPLICIT', hardBlockerCandidate: false, blockerCondition: null,
      constraint: null, extractionConfidence: null });
    this.goToRequirement((this.draft?.requirements.length ?? 1) - 1);
  }
  save(): void { if (this.draft) this.submit({ action: 'SAVE', extraction: this.draft }); }
  bypass(): void { if (!this.dirty()) this.submit({ action: 'BYPASS' }); }
  private submit(command: ReviewCommand): void {
    const offer = this.offer();
    if (!offer || this.saving()) return;
    this.saving.set(true); this.error.set(''); this.fieldErrors.set([]); this.success.set(false);
    this.service.review(offer.id, command).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.accept(saved); this.saving.set(false); this.success.set(true);
        if (command.action === 'BYPASS') void this.router.navigate(['/job-offers', saved.id]);
      },
      error: failure => {
        this.saving.set(false);
        this.error.set('La vérification n’a pas été enregistrée. Vos modifications sont conservées.');
        this.fieldErrors.set(Object.values(failure.error?.fieldErrors ?? {}));
      },
    });
  }
}
