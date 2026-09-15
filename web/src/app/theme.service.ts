import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';
export const THEME_STORAGE_KEY = 'roledock.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly preference = signal<Theme>('light');
  readonly theme = this.preference.asReadonly();

  constructor() {
    let theme: Theme = 'light';
    try {
      if (this.document.defaultView?.localStorage.getItem(THEME_STORAGE_KEY) === 'dark') {
        theme = 'dark';
      }
    } catch {
      // Storage may be blocked; switching still works for this session.
    }
    this.apply(theme);
  }

  setTheme(theme: Theme): void {
    this.apply(theme);
    try {
      this.document.defaultView?.localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // An unavailable preference store must not prevent using the application.
    }
  }

  private apply(theme: Theme): void {
    this.preference.set(theme);
    this.document.documentElement.dataset['theme'] = theme;
  }
}
