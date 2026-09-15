import { TestBed } from '@angular/core/testing';
import { ThemeService, THEME_STORAGE_KEY } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.removeItem(THEME_STORAGE_KEY);
    document.documentElement.removeAttribute('data-theme');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.removeItem(THEME_STORAGE_KEY);
    document.documentElement.removeAttribute('data-theme');
  });

  it('switches both ways immediately and persists each choice', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('light');
    for (const theme of ['dark', 'light'] as const) {
      service.setTheme(theme);
      expect(service.theme()).toBe(theme);
      expect(document.documentElement.dataset['theme']).toBe(theme);
      expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe(theme);
    }
  });

  it.each(['dark', 'light'] as const)('restores persisted %s on application startup', theme => {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe(theme);
    expect(document.documentElement.dataset['theme']).toBe(theme);
  });

  it('defaults to light for an invalid stored value', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'invalid');
    expect(TestBed.inject(ThemeService).theme()).toBe('light');
  });

  it('remains usable when storage access fails', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('light');
    expect(() => service.setTheme('dark')).not.toThrow();
    expect(document.documentElement.dataset['theme']).toBe('dark');
  });
});
