import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JobOffer } from './job-offer.models';
import { JobOfferService } from './job-offer.service';
import { JobOfferPreviewComponent } from './job-offer-preview.component';

@Component({
  selector: 'app-job-offer-detail',
  imports: [RouterLink, JobOfferPreviewComponent],
  template: `
    <header class="app-header"><a class="brand" routerLink="/profile">RoleDock</a><a routerLink="/job-offers/new">Nouvelle offre</a></header>
    <main class="page-shell">
      <h1>Offre enregistrée en brouillon</h1>
      @if (loading()) { <p role="status">Chargement…</p> }
      @if (error()) { <p role="alert">Impossible de charger cette offre. Vérifiez le lien et la disponibilité du serveur.</p><button (click)="load()">Réessayer</button> }
      @if (offer(); as saved) {
        <section class="form-section">
          @if (saved.reviewStatus === 'UNREVIEWED') { <p class="notice">Analyse automatique — vérification recommandée.</p> }
          <app-job-offer-preview [extraction]="saved.extraction" />
          @if (saved.sourceUrl) { <p>Source : <a [href]="saved.sourceUrl" target="_blank" rel="noopener noreferrer">{{ saved.sourceUrl }}</a></p> }
          <details><summary>Annonce originale</summary><pre style="white-space: pre-wrap; overflow-wrap: anywhere">{{ saved.originalText }}</pre></details>
        </section>
      }
    </main>
  `,
})
export class JobOfferDetailComponent {
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
