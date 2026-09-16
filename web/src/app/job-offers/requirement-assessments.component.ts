import { Component, DestroyRef, computed, inject, input, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JobOfferService } from './job-offer.service';
import { JobOffer } from './job-offer.models';
import { labels } from './job-offer-preview.component';
import { OfferAnalysis } from './requirement-assessment.models';
import { groupRequirements } from './requirement-groups';

@Component({
  selector: 'app-requirement-assessments',
  template: `
    <section class="form-section" aria-labelledby="matching-title">
      <div class="section-heading"><h2 id="matching-title">Votre profil face aux exigences</h2>
        <button type="button" class="secondary" [disabled]="loading()" (click)="load()">Actualiser</button></div>
      <p>Évaluation des informations enregistrées dans votre profil. Une information absente reste à vérifier.</p>
      @if ((result()?.reviewStatus ?? offer().reviewStatus) === 'UNREVIEWED') { <p class="notice">Analyse non vérifiée : les conditions bloquantes nécessitent une vérification.</p> }
      @if (loading()) { <p role="status">Évaluation en cours…</p> }
      @if (error()) { <p role="alert">Impossible de charger l’évaluation. Réessayez avec le bouton Actualiser.</p> }
      @if (result(); as response) {
        <p>Évaluation au {{ response.assessedOn }}</p>
        <section class="analysis-summary item-card" aria-label="Synthèse de l’analyse">
          <p class="coverage"><strong>Couverture des exigences : {{ response.coverageScore == null ? 'Indisponible' : numberFormat.format(response.coverageScore) + ' %' }}</strong></p>
          <p>Mesure la couverture des exigences connues de l’offre, pas vos chances d’obtenir un entretien.</p>
          <p>Éligibilité : {{ eligibilityLabels[response.eligibility] ?? 'Indéterminée' }}</p>
          <p class="recommendation"><strong>{{ recommendationLabels[response.recommendation] ?? 'À vérifier avant de décider' }}</strong></p>
          @if (response.criticalGaps.length) {
            <section class="critical-gaps notice" aria-label="Points critiques">
              <h3>Points critiques</h3>
              <ul>@for (gap of response.criticalGaps; track gap.requirementId) {
                <li><strong>{{ gap.label }}</strong> — requis / cœur — {{ statusLabels[gap.status] ?? 'À vérifier' }}<br>{{ gap.rationale }}</li>
              }</ul>
            </section>
          }
          <section class="uncertainty" aria-label="Informations à vérifier">
            <h3>Informations à vérifier</h3>
            <p>Incertitude : {{ uncertaintyLabels[response.uncertainty.level] ?? 'non déterminée' }}. Cet indicateur décrit les informations incomplètes ou ambiguës.</p>
            <ul>@for (reason of response.uncertainty.reasons; track reason) { <li>{{ reason }}</li> }
              @empty { <li>Aucune incertitude signalée par les règles actuelles.</li> }</ul>
          </section>
        </section>
        @for (group of groups(); track group.category) {
          <h3>{{ categoryLabels[group.category] ?? 'Autre' }}</h3>
          <div class="assessment-grid">
            @for (entry of group.entries; track entry.index) {
              @if (byId().get(entry.requirement.id ?? ''); as assessment) {
                <article class="item-card" [attr.data-status]="assessment.status">
                  <h4>{{ entry.requirement.canonicalLabel }}</h4>
                  <p>{{ kindLabels[entry.requirement.requirementKind] ?? 'Non précisé' }} · <strong>{{ statusLabels[assessment.status] ?? 'À vérifier' }}</strong></p>
                  @if (assessment.transferRelation !== 'NONE') { <p>{{ relationLabels[assessment.transferRelation] ?? 'Relation à vérifier' }}</p> }
                  <p>{{ assessment.rationale }}</p>
                  @if (contributionsById().get(assessment.requirementId); as contribution) {
                    <p class="evidence-meta">{{ contribution.rationale }}
                      @if (contribution.included) { Poids : {{ numberFormat.format(contribution.weight) }} · Couverture : {{ contribution.coverage === null ? 'Indisponible' : numberFormat.format(contribution.coverage) }} (sur 1). }
                    </p>
                  }
                  <p class="evidence-meta">Preuve : {{ strengthLabels[assessment.evidenceStrength] ?? 'non précisée' }} · Confiance : {{ confidenceLabels[assessment.assessmentConfidence] ?? 'non précisée' }}</p>
                  @if (assessment.eligibilityEffect !== 'NONE') {
                    <p class="notice">{{ assessment.eligibilityEffect === 'BLOCK' ? 'Incompatibilité explicite avec cette exigence' : 'Blocage possible — à vérifier' }}</p>
                  }
                  @if (assessment.attention) { <p>{{ assessment.attention }}</p> }
                  <ul>
                    @for (evidence of assessment.evidence; track evidence.type + evidence.id) {
                      <li>{{ evidenceLabels[evidence.type] ?? 'Élément du profil' }} : {{ evidence.label }}</li>
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
    .analysis-summary { margin: 1rem 0 1.5rem; }
    .coverage { font-size: 1.15rem; }
    .critical-gaps, .uncertainty { margin-top: 1rem; padding-top: .5rem; border-top: 1px solid var(--border-default); }
  `,
})
export class RequirementAssessmentsComponent implements OnInit {
  readonly offer = input.required<JobOffer>();
  private readonly service = inject(JobOfferService);
  private readonly destroyRef = inject(DestroyRef);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly result = signal<OfferAnalysis | null>(null);
  readonly groups = computed(() => groupRequirements(this.offer().extraction.requirements));
  readonly byId = computed(() => new Map(this.result()?.requirementAssessments.map(a => [a.requirementId, a]) ?? []));
  readonly contributionsById = computed(() => new Map(this.result()?.contributions.map(c => [c.requirementId, c]) ?? []));
  readonly numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
  readonly eligibilityLabels: Partial<Record<string, string>> = { ELIGIBLE: 'Aucun blocage identifié', ELIGIBLE_WITH_CONSTRAINT: 'Sous réserve de vérification', NOT_ELIGIBLE: 'Critère bloquant confirmé', UNKNOWN: 'Indéterminée' };
  readonly recommendationLabels: Partial<Record<string, string>> = { APPLY_NOW: 'Candidature pertinente', APPLY_WITH_BRIDGE: 'Candidature pertinente avec points à expliquer', STRETCH: 'Candidature ambitieuse', VERIFY_FIRST: 'À vérifier avant de décider', SKIP_CONFIRMED_BLOCKER: 'Critère bloquant confirmé' };
  readonly uncertaintyLabels: Partial<Record<string, string>> = { LOW: 'faible', MEDIUM: 'modérée', HIGH: 'élevée' };
  readonly categoryLabels: Partial<Record<string, string>> = labels;
  readonly kindLabels: Partial<Record<string, string>> = labels;
  readonly statusLabels: Partial<Record<string, string>> = { MATCH: 'Couvert', PARTIAL: 'Partiellement couvert', MISSING: 'Non couvert', UNKNOWN: 'À vérifier', NOT_APPLICABLE: 'Non applicable' };
  readonly relationLabels: Partial<Record<string, string>> = { EXACT: 'Correspondance exacte', EQUIVALENT: 'Équivalence explicite', ADJACENT: 'Compétence voisine', PREREQUISITE: 'Prérequis', NONE: '' };
  readonly strengthLabels: Partial<Record<string, string>> = { STRONG: 'forte', MODERATE: 'modérée', WEAK: 'faible', NONE: 'aucune' };
  readonly confidenceLabels: Partial<Record<string, string>> = { HIGH: 'élevée', MEDIUM: 'modérée', LOW: 'faible' };
  readonly evidenceLabels: Partial<Record<string, string>> = { PROFILE_SKILL: 'Compétence', EXPERIENCE: 'Expérience', EDUCATION: 'Formation', LANGUAGE: 'Langue', CERTIFICATION: 'Certification', CERTIFICATION_LIST: 'Certifications du profil', SIGNIFICANT_PROJECT: 'Projet' };
  ngOnInit(): void { this.load(); }
  load(): void {
    if (this.loading()) return;
    this.loading.set(true); this.error.set(false); this.result.set(null);
    this.service.analysis(this.offer().id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.result.set(result); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
