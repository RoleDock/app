import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then((module) => module.ProfileComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'profile' },
  { path: '**', redirectTo: 'profile' },
];
