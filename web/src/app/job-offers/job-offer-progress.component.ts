import { Component, computed, input } from '@angular/core';
import { JobOffer } from './job-offer.models';

export type AnalysisState = 'idle' | 'processing' | 'error' | 'completed';
type StepState = 'pending' | 'current' | 'processing' | 'error' | 'completed' | 'bypassed';

@Component({
  selector: 'app-job-offer-progress',
  templateUrl: './job-offer-progress.component.html',
  styleUrl: './job-offer-progress.component.scss',
})
export class JobOfferProgressComponent {
  readonly page = input.required<'new' | 'review' | 'detail'>();
  readonly analysisState = input<AnalysisState>('idle');
  readonly reviewStatus = input<JobOffer['reviewStatus'] | null>(null);
  readonly reviewBypassedAt = input<string | null>(null);
  readonly editing = input(false);

  readonly steps = computed(() => {
    const persisted = this.reviewStatus() !== null || this.analysisState() === 'completed';
    const reviewed = this.reviewStatus() === 'CONFIRMED' || this.reviewStatus() === 'CORRECTED';
    const bypassed = !reviewed && this.reviewStatus() === 'UNREVIEWED' && !!this.reviewBypassedAt();
    const ready = persisted && !this.editing() && (this.page() === 'detail' || reviewed || bypassed);
    const reviewing = persisted && !ready;
    const analysis = this.analysisState();
    const states: StepState[] = [
      persisted || analysis !== 'idle' ? 'completed' : this.page() === 'new' ? 'current' : 'pending',
      persisted ? 'completed' : analysis === 'idle' ? 'pending' : analysis,
      reviewed ? 'completed' : bypassed ? 'bypassed' : reviewing ? 'current' : 'pending',
      ready ? 'current' : 'pending',
    ];
    return ['Offre', 'Analyse', 'Vérification', 'Offre prête'].map((label, index) => ({
      label, state: states[index],
      current: index === 0 ? states[0] === 'current' : index === 1 ? !persisted && analysis !== 'idle' : index === 2 ? reviewing : ready,
      description: index === 2
        ? reviewed ? this.reviewStatus() === 'CORRECTED' ? 'Vérifiée et corrigée' : 'Vérifiée'
          : bypassed ? 'Ignorée · non vérifiée' : reviewing ? 'Recommandée · facultative' : 'Facultative'
        : ({ pending: 'À suivre', current: 'Étape actuelle', processing: 'En cours…', error: 'À réessayer', completed: 'Terminée', bypassed: 'Ignorée' } satisfies Record<StepState, string>)[states[index]],
    }));
  });
}
