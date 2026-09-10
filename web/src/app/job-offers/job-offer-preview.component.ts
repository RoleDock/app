import { Component, computed, input } from '@angular/core';
import { Extraction, CurrentExtraction, Requirement, CurrentRequirement } from './job-offer.models';
import { groupRequirements } from './requirement-groups';

@Component({
  selector: 'app-job-offer-preview',
  template: `
    @let data = extraction();
    <section aria-label="Résultat structuré">
      <h2>{{ data.position ?? 'Poste non précisé' }}</h2>
      <dl class="metadata">
        <div><dt>Entreprise</dt><dd>{{ data.company ?? 'Non précisée' }}</dd></div>
        <div><dt>Localisation</dt><dd>{{ [data.location.city, data.location.region, data.location.country].filter(present).join(', ') || 'Non précisée' }}</dd></div>
        <div><dt>Contrat</dt><dd>{{ label(data.contractType) }}</dd></div>
        <div><dt>Organisation du travail</dt><dd>{{ label(data.workArrangement.type) }}
          @if (data.workArrangement.remoteArea) { · {{ data.workArrangement.remoteArea }} }
          @if (data.workArrangement.onSiteDaysPerWeek !== null) { · {{ data.workArrangement.onSiteDaysPerWeek }} jour(s) sur site / semaine }
        </dd></div>
      </dl>
      <div class="overview">
        <section class="overview-section" aria-label="Résumé">
          <h3>Le poste en bref</h3><p>{{ data.summary ?? 'Non précisé' }}</p>
        </section>
        <section class="overview-section" aria-label="Missions">
          <h3>Vos missions</h3>
          <ul class="missions">@for (mission of data.missions; track $index) { <li>{{ mission }}</li> } @empty { <li>Aucune mission extraite.</li> }</ul>
        </section>
      </div>
      <section class="requirements-section" aria-label="Exigences">
        <div class="requirements-heading"><h3>Exigences du poste</h3><span class="count">{{ data.requirements.length }}</span></div>
        @for (group of groups(); track group.category) {
          <section class="requirement-category">
            <h4 class="category-heading"><button type="button" class="category-toggle" [attr.aria-expanded]="openCategories.has(group.category)" [attr.aria-controls]="'offer-category-' + group.category" (click)="toggleCategory(group.category)"><span>{{ label(group.category) }}</span><span class="category-count">{{ group.entries.length }}</span><span class="category-chevron" aria-hidden="true">{{ openCategories.has(group.category) ? '−' : '+' }}</span></button></h4>
            <div class="category-content" [id]="'offer-category-' + group.category" [hidden]="!openCategories.has(group.category)">
            <div class="requirements-grid">
          @for (entry of group.entries; track entry.index) {
            @let requirement = entry.requirement;
            <article class="requirement">
              <p class="category">{{ label(requirement.category) }}</p>
              <h4>{{ requirement.canonicalLabel }}</h4>
              <div class="badges">
                <span class="badge" [class.required]="requirement.requirementKind === 'REQUIRED'">{{ label(requirement.requirementKind) }}</span>
                <span class="priority">Importance : {{ label(requirement.centrality) }}</span>
              </div>
              @if (requirement.rawText) {
                <div class="source-quote"><p class="source-label">Dans l’annonce</p><blockquote>{{ requirement.rawText }}</blockquote></div>
              } @else { <p class="manual-source">Ajoutée par vous — sans citation automatique</p> }
            </article>
          }
            </div>
            </div>
          </section>
        } @empty { <p class="empty-list">Aucune exigence extraite.</p> }
      </section>
    </section>
  `,
  styleUrl: './job-offer-preview.component.scss',
})
export class JobOfferPreviewComponent {
  readonly extraction = input.required<Extraction | CurrentExtraction>();
  readonly groups = computed(() => groupRequirements<Requirement | CurrentRequirement>(this.extraction().requirements));
  readonly openCategories = new Set<string>();
  toggleCategory(category: string): void {
    if (this.openCategories.has(category)) this.openCategories.delete(category);
    else this.openCategories.add(category);
  }
  readonly present = (value: string | null) => value !== null && value !== '';
  label(value: string): string { return labels[value] ?? value; }
}
export const labels: Record<string, string> = {
  UNKNOWN: 'Non précisé', ONSITE: 'Sur site', HYBRID: 'Hybride', REMOTE: 'À distance',
  PERMANENT: 'Contrat permanent', FIXED_TERM: 'Durée déterminée', FREELANCE: 'Indépendant',
  INTERNSHIP: 'Stage', APPRENTICESHIP: 'Alternance', TEMPORARY: 'Travail temporaire', OTHER: 'Autre',
  REQUIRED: 'Requis', PREFERRED: 'Souhaité', CONTEXTUAL: 'Contexte',
  CORE: 'Central', SUPPORTING: 'Complémentaire', INCIDENTAL: 'Périphérique',
  TECH_SKILL: 'Compétence technique', EXPERIENCE: 'Expérience', DOMAIN_KNOWLEDGE: 'Connaissance métier',
  TITLE_LEVEL: 'Niveau du poste', EDUCATION: 'Formation', CERTIFICATION: 'Certification',
  LOCATION: 'Localisation', WORK_AUTHORIZATION: 'Autorisation de travail', LANGUAGE: 'Langue',
  EXPLICIT: 'Explicite', INFERRED: 'Interprétée', AT_LEAST: 'Au moins', AT_MOST: 'Au plus', EQUALS: 'Égal à', RANGE: 'Intervalle',
  AVAILABILITY: 'Disponibilité', CONTRACT: 'Contrat', SOFT_SKILL: 'Compétence relationnelle',
};
