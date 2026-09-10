import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter, Router } from '@angular/router';

import { App } from './app';

@Component({ template: '' })
class TestPage {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([
        { path: 'profile', component: TestPage },
        { path: 'job-offers/new', component: TestPage },
        { path: 'job-offers/:id/review', component: TestPage },
      ])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should provide the application router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });

  it('keeps the same navigation across pages and identifies the current destination', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/profile');
    await fixture.whenStable();
    const links = element.querySelectorAll<HTMLAnchorElement>('nav a');
    expect(links.length).toBe(2);
    expect(links[0].getAttribute('aria-current')).toBe('page');
    expect(links[1].hasAttribute('aria-current')).toBe(false);

    links[1].click();
    await fixture.whenStable();
    expect(router.url).toBe('/job-offers/new');
    expect(links[1].getAttribute('aria-current')).toBe('page');
    expect(links[0].hasAttribute('aria-current')).toBe(false);

    await router.navigateByUrl('/job-offers/demo/review');
    await fixture.whenStable();
    expect(element.querySelectorAll('header')).toHaveLength(1);
    expect(element.querySelectorAll('nav a')).toHaveLength(2);
    expect(element.querySelector('nav [aria-current]')).toBeNull();
  });
});
