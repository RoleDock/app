import { Component, DestroyRef, computed, inject, input, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JobOfferService } from './job-offer.service';
import { JobOffer } from './job-offer.models';
import { labels } from './job-offer-preview.component';
import { AssessmentResponse } from './requirement-assessment.models';
import { groupRequirements } from './requirement-groups';

@Component({
  selector: 'app-requirement-assessments',
  template: `
    <section class="form-section" aria-labelledby="matching-title">
      <div class="section-heading"><h2 id="matching-title">Votre profil face aux exigences</h2>
        <button type="button" class="secondary" [disabled]="loading()" (click)="load()">Actualiser</button></div>
      <p>Évaluation des informations enregistrées dans votre profil. Une information absente reste à vérifier.</p>
      @if (offer().reviewStatus === 'UNREVIEWED') { <p class="notice">Analyse non vérifiée : les conditions bloquantes nécessitent une vérification.</p> }
      @if (loading()) { <p role="status">Évaluation en cours…</p> }
      @if (error()) { <p role="alert">Impossible de charger l’évaluation. Réessayez avec le bouton Actualiser.</p> }
      @if (result(); as response) {
        <p>Évaluation au {{ response.assessedOn }}</p>
        @for (group of groups(); track group.category) {
          <h3>{{ categoryLabels[group.category] }}</h3>
          <div class="assessment-grid">
            @for (entry of group.entries; track entry.index) {
              @if (byId().get(entry.requirement.id ?? ''); as assessment) {
                <article class="item-card" [attr.data-status]="assessment.status">
                  <h4>{{ entry.requirement.canonicalLabel }}</h4>
                  <p>{{ kindLabels[entry.requirement.requirementKind] }} · <strong>{{ statusLabels[assessment.status] }}</strong></p>
                  @if (assessment.transferRelation !== 'NONE') { <p>{{ relationLabels[assessment.transferRelation] }}</p> }
                  <p>{{ assessment.rationale }}</p>
                  <p class="evidence-meta">Preuve : {{ strengthLabels[assessment.evidenceStrength] }} · Confiance : {{ confidenceLabels[assessment.assessmentConfidence] }}</p>
                  @if (assessment.eligibilityEffect !== 'NONE') {
                    <p class="notice">{{ assessment.eligibilityEffect === 'BLOCK' ? 'Incompatibilité explicite avec cette exigence' : 'Blocage possible — à vérifier' }}</p>
                  }
                  @if (assessment.attention) { <p>{{ assessment.attention }}</p> }
                  <ul>
                    @for (evidence of assessment.evidence; track evidence.type + evidence.id) {
                      <li>{{ evidenceLabels[evidence.type] }} : {{ evidence.label }}</li>
                    } @empty { <li>Aucune preuve référencée.</li> }
                  </ul>
                </article>
              }
            }
          </div>
        } @empty { <p>Aucune exigence à évaluer pour cette offre.</p> }
      }
    </section>
  `,
  styles: `
    :host { display: block; margin-top: 1.5rem; }
    .assessment-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 24rem), 1fr)); gap: 1rem; }
    .item-card { padding: 1rem; border: 1px solid var(--border-default); border-radius: 12px; overflow-wrap: anywhere; background: var(--surface-card); }
    h4 { font-size: 1rem; margin: 0 0 .75rem; }
    ul { padding-left: 1.25rem; }
    .evidence-meta { font-size: .875rem; }
  `,
})
export class RequirementAssessmentsComponent implements OnInit {
  readonly offer = input.required<JobOffer>();
  private readonly service = inject(JobOfferService);
  private readonly destroyRef = inject(DestroyRef);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly result = signal<AssessmentResponse | null>(null);
  readonly groups = computed(() => groupRequirements(this.offer().extraction.requirements));
  readonly byId = computed(() => new Map(this.result()?.assessments.map(a => [a.requirementId, a]) ?? []));
  readonly categoryLabels = labels;
  readonly kindLabels = labels;
  readonly statusLabels = { MATCH: 'Couvert', PARTIAL: 'Partiellement couvert', MISSING: 'Non couvert', UNKNOWN: 'À vérifier', NOT_APPLICABLE: 'Non applicable' };
  readonly relationLabels = { EXACT: 'Correspondance exacte', EQUIVALENT: 'Équivalence explicite', ADJACENT: 'Compétence voisine', PREREQUISITE: 'Prérequis', NONE: '' };
  readonly strengthLabels = { STRONG: 'forte', MODERATE: 'modérée', WEAK: 'faible', NONE: 'aucune' };
  readonly confidenceLabels = { HIGH: 'élevée', MEDIUM: 'modérée', LOW: 'faible' };
  readonly evidenceLabels = { PROFILE_SKILL: 'Compétence', EXPERIENCE: 'Expérience', EDUCATION: 'Formation', LANGUAGE: 'Langue', CERTIFICATION: 'Certification', CERTIFICATION_LIST: 'Certifications du profil', SIGNIFICANT_PROJECT: 'Projet' };
  ngOnInit(): void { this.load(); }
  load(): void {
    if (this.loading()) return;
    this.loading.set(true); this.error.set(false); this.result.set(null);
    this.service.assessments(this.offer().id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.result.set(result); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
