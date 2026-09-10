import { CanDeactivateFn, Routes } from '@angular/router';

import type { JobOfferReviewComponent } from './job-offers/job-offer-review.component';

export const leaveReview: CanDeactivateFn<JobOfferReviewComponent> = component => component.canLeave();

export const routes: Routes = [
  { path: 'job-offers/new', loadComponent: () => import('./job-offers/new-job-offer.component').then(m => m.NewJobOfferComponent) },
  { path: 'job-offers/:id/review', canDeactivate: [leaveReview], loadComponent: () => import('./job-offers/job-offer-review.component').then(m => m.JobOfferReviewComponent) },
  { path: 'job-offers/:id', loadComponent: () => import('./job-offers/job-offer-detail.component').then(m => m.JobOfferDetailComponent) },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((module) => module.ProfileComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'profile' },
  { path: '**', redirectTo: 'profile' },
];
