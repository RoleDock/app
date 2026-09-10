import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'job-offers/new', loadComponent: () => import('./job-offers/new-job-offer.component').then(m => m.NewJobOfferComponent) },
  { path: 'job-offers/:id', loadComponent: () => import('./job-offers/job-offer-detail.component').then(m => m.JobOfferDetailComponent) },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((module) => module.ProfileComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'profile' },
  { path: '**', redirectTo: 'profile' },
];
