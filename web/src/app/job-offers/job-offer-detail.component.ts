import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JobOffer, reviewLabel } from './job-offer.models';
import { JobOfferService } from './job-offer.service';
import { JobOfferPreviewComponent } from './job-offer-preview.component';
import { JobOfferProgressComponent } from './job-offer-progress.component';
import { RequirementAssessmentsComponent } from './requirement-assessments.component';

@Component({
  selector: 'app-job-offer-detail',
  imports: [RouterLink, JobOfferPreviewComponent, JobOfferProgressComponent, RequirementAssessmentsComponent],
  template: `
    <main class="page-shell">
      <app-job-offer-progress page="detail" [reviewStatus]="offer()?.reviewStatus ?? null" [reviewBypassedAt]="offer()?.reviewBypassedAt ?? null" />
      <div class="page-heading"><div><p class="eyebrow">Votre espace candidature</p><h1>Votre offre d’emploi</h1></div></div>
      @if (loading()) { <p role="status">Chargement…</p> }
      @if (error()) { <p role="alert">Impossible de charger cette offre. Vérifiez le lien et la disponibilité du serveur.</p><button class="secondary" (click)="load()">Réessayer</button> }
      @if (offer(); as saved) {
        <section class="form-section">
          <p class="notice" role="status">{{ reviewLabel(saved) }}</p>
          <div class="offer-actions"><a class="primary" [routerLink]="['/job-offers', saved.id, 'review']">Vérifier ou corriger l’analyse</a></div>
          <app-job-offer-preview [extraction]="saved.extraction" />
          @if (saved.sourceUrl) { <p>Source : <a class="secondary" [href]="saved.sourceUrl" target="_blank" rel="noopener noreferrer">{{ saved.sourceUrl }}</a></p> }
          <details><summary>Annonce originale</summary><pre style="white-space: pre-wrap; overflow-wrap: anywhere">{{ saved.originalText }}</pre></details>
        </section>
        <app-requirement-assessments [offer]="saved" />
      }
    </main>
  `,
})
export class JobOfferDetailComponent {
  readonly reviewLabel = reviewLabel;
  private readonly service = inject(JobOfferService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  readonly offer = signal<JobOffer | null>(null);
  readonly loading = signal(false);
  readonly error = signal(false);
  constructor() { this.load(); }
  load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(false);
    this.service.get(this.route.snapshot.paramMap.get('id') ?? '').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: offer => { this.offer.set(offer); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
