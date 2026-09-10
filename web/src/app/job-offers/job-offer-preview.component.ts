import { Component, input } from '@angular/core';
import { Extraction } from './job-offer.models';

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
      <h3>Résumé</h3><p>{{ data.summary ?? 'Non précisé' }}</p>
      <h3>Missions</h3>
      <ul>@for (mission of data.missions; track $index) { <li>{{ mission }}</li> } @empty { <li>Aucune mission extraite.</li> }</ul>
      <h3>Exigences</h3>
      @for (requirement of data.requirements; track $index) {
        <article class="requirement">
          <h4>{{ requirement.canonicalLabel }}</h4>
          <p>{{ label(requirement.requirementKind) }} · {{ label(requirement.centrality) }} · {{ label(requirement.category) }}</p>
          <blockquote>{{ requirement.rawText }}</blockquote>
        </article>
      } @empty { <p>Aucune exigence extraite.</p> }
    </section>
  `,
  styles: `
    .metadata { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    dt { font-weight: 600; } dd { margin: .3rem 0; }
    .requirement { border-top: 1px solid #d7dfd9; padding: .7rem 0; }
    h4 { margin-bottom: .4rem; } blockquote { margin: .6rem 0; padding-left: 1rem; border-left: 3px solid #749883; white-space: pre-wrap; }
  `,
})
export class JobOfferPreviewComponent {
  readonly extraction = input.required<Extraction>();
  readonly present = (value: string | null) => value !== null && value !== '';
  label(value: string): string { return labels[value] ?? value; }
}
const labels: Record<string, string> = {
  UNKNOWN: 'Non précisé', ONSITE: 'Sur site', HYBRID: 'Hybride', REMOTE: 'À distance',
  PERMANENT: 'Contrat permanent', FIXED_TERM: 'Durée déterminée', FREELANCE: 'Indépendant',
  INTERNSHIP: 'Stage', APPRENTICESHIP: 'Alternance', TEMPORARY: 'Travail temporaire', OTHER: 'Autre',
  REQUIRED: 'Requis', PREFERRED: 'Souhaité', CONTEXTUAL: 'Contexte',
  CORE: 'Central', SUPPORTING: 'Complémentaire', INCIDENTAL: 'Périphérique',
  TECH_SKILL: 'Compétence technique', EXPERIENCE: 'Expérience', DOMAIN_KNOWLEDGE: 'Connaissance métier',
  TITLE_LEVEL: 'Niveau du poste', EDUCATION: 'Formation', CERTIFICATION: 'Certification',
  LOCATION: 'Localisation', WORK_AUTHORIZATION: 'Autorisation de travail', LANGUAGE: 'Langue',
  AVAILABILITY: 'Disponibilité', CONTRACT: 'Contrat', SOFT_SKILL: 'Compétence relationnelle',
};
