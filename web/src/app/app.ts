import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';

import { HealthService } from './health.service';

@Component({
  selector: 'app-root',
  imports: [AsyncPipe, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly backendStatus$ = inject(HealthService)
    .getStatus()
    .pipe(catchError(() => of('UNAVAILABLE')));
}
