import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { App } from './app';
import { HealthService } from './health.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: HealthService,
          useValue: { getStatus: () => of('UP') },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the backend status supplied by the health service', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('RoleDock');
    expect(compiled.textContent).toContain('Backend status: UP');
  });
});
