import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Analysis, JobOffer } from './job-offer.models';
import { JobOfferService } from './job-offer.service';
import { JobOfferPreviewComponent } from './job-offer-preview.component';

@Component({
  selector: 'app-new-job-offer',
  imports: [ReactiveFormsModule, RouterLink, JobOfferPreviewComponent],
  templateUrl: './new-job-offer.component.html',
  styles: `.offer-text-help { color: #66736e; font-size: .8rem; } .analysis-progress { display: flex; align-items: center; gap: .8rem; color: #384640; } .analysis-progress .spinner { flex-shrink: 0; } .preview-save { margin-top: 0; }`,
})
export class NewJobOfferComponent {
  private readonly service = inject(JobOfferService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly form = new FormGroup({
    originalText: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(50000), Validators.pattern(/\S/)] }),
    sourceUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000), Validators.pattern(/^https?:\/\/\S+$/)] }),
  });
  readonly analyzing = signal(false);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly analysis = signal<Analysis | null>(null);
  readonly saved = signal<JobOffer | null>(null);

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.analysis.set(null);
      this.saved.set(null);
      this.error.set('');
    });
  }

  analyze(): void {
    if (this.analyzing() || this.saving()) return;
    this.submitted.set(true);
    if (this.form.invalid) return;
    this.error.set('');
    this.analysis.set(null);
    this.saved.set(null);
    this.analyzing.set(true);
    this.form.disable({ emitEvent: false });
    const { originalText, sourceUrl } = this.form.getRawValue();
    this.service.analyze(originalText, sourceUrl || null).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.analysis.set(result);
        this.analyzing.set(false);
        this.form.enable({ emitEvent: false });
      },
      error: () => {
        this.analyzing.set(false);
        this.form.enable({ emitEvent: false });
        this.error.set('L’analyse est indisponible ou a échoué. Votre saisie est conservée. Réessayez.');
      },
    });
  }

  save(): void {
    const analysis = this.analysis();
    if (!analysis || this.saving() || this.analyzing() || this.saved()) return;
    this.saving.set(true);
    this.error.set('');
    this.form.disable({ emitEvent: false });
    this.service.save(analysis.analysisId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.saved.set(result);
        void this.router.navigate(['/job-offers', result.id]);
        this.saving.set(false);
        this.form.enable({ emitEvent: false });
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.form.enable({ emitEvent: false });
        if (error.status === 409) {
          this.analysis.set(null);
          this.error.set('Cette analyse a expiré ou a été remplacée. Votre saisie est conservée. Relancez l’analyse.');
        } else {
          this.error.set('L’enregistrement a échoué. Votre saisie et le résultat sont conservés. Réessayez.');
        }
      },
    });
  }
}
