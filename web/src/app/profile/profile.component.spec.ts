import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { CandidateProfile, ProfilePayload } from './profile.models';
import { ProfileComponent } from './profile.component';
import { ProfileService } from './profile.service';

describe('ProfileComponent', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let load$: Subject<CandidateProfile | null>;
  let save$: Subject<CandidateProfile>;
  let service: { getCurrent: ReturnType<typeof vi.fn>; save: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    load$ = new Subject<CandidateProfile | null>();
    save$ = new Subject<CandidateProfile>();
    service = { getCurrent: vi.fn(() => load$), save: vi.fn(() => save$) };
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [provideRouter([]), { provide: ProfileService, useValue: service }],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
  });

  it('shows loading then populates an existing profile including explicit links', () => {
    expect(element('[data-testid="loading-state"]')).not.toBeNull();
    load$.next(existingProfile);
    load$.complete();
    fixture.detectChanges();

    expect(valueOf('input[formcontrolname="mainTitle"]')).toBe('Backend engineer');
    expect(documentElements('[data-testid="skill-item"]')).toHaveLength(1);
    expect(documentElements('[data-testid="experience-item"]')).toHaveLength(1);
    const linkedSkill = element('.skill-links input') as HTMLInputElement;
    expect(linkedSkill.checked).toBe(true);
  });

  it('handles the absence of a profile with an empty editable form', () => {
    finishLoad(null);
    expect(element('[data-testid="empty-profile"]')?.textContent).toContain('Aucun profil enregistré');
    expect(valueOf('input[formcontrolname="mainTitle"]')).toBe('');
    expect(documentElements('[data-testid="experience-item"]')).toHaveLength(0);
  });

  it('adds and removes repeatable items and sends explicit skill attachments', () => {
    finishLoad(null);
    click('[data-testid="add-skill"]');
    setInput('[data-testid="skill-item"] input[formcontrolname="name"]', 'TypeScript');
    click('[data-testid="add-experience"]');

    const checkbox = element('.skill-links input') as HTMLInputElement;
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    submit();

    expect(service.save).toHaveBeenCalledOnce();
    const payload = service.save.mock.calls[0][0] as ProfilePayload;
    expect(payload.experiences[0].skillIds).toEqual([payload.skills[0].id]);

    click('[data-testid="skill-item"] .remove');
    expect(documentElements('[data-testid="skill-item"]')).toHaveLength(0);
  });

  it('shows local validation errors and does not submit invalid data', () => {
    finishLoad(null);
    click('[data-testid="add-skill"]');
    submit();

    expect(element('.field-error')?.textContent).toContain('nom de la compétence');
    expect(element('[data-testid="validation-summary"]')).not.toBeNull();
    expect(service.save).not.toHaveBeenCalled();
  });

  it('exposes saving and success states', () => {
    finishLoad(null);
    setInput('input[formcontrolname="mainTitle"]', 'Platform engineer');
    submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement…');

    save$.next({ ...emptyPayload, id: 'profile-id', mainTitle: 'Platform engineer' });
    save$.complete();
    fixture.detectChanges();
    expect(element('[data-testid="save-success"]')?.textContent).toContain('succès');
  });

  it('keeps entered data after a save failure', () => {
    finishLoad(null);
    setInput('input[formcontrolname="mainTitle"]', 'Unsaved title');
    submit();
    save$.error(new Error('offline'));
    fixture.detectChanges();

    expect(element('[data-testid="save-error"]')?.textContent).toContain('saisie est conservée');
    expect(valueOf('input[formcontrolname="mainTitle"]')).toBe('Unsaved title');
  });

  it('shows a load failure and can retry', () => {
    load$.error(new Error('offline'));
    fixture.detectChanges();
    expect(element('[data-testid="load-error"]')).not.toBeNull();

    load$ = new Subject<CandidateProfile | null>();
    service.getCurrent.mockReturnValue(load$);
    click('[data-testid="load-error"] button');
    expect(service.getCurrent).toHaveBeenCalledTimes(2);
  });

  function finishLoad(profile: CandidateProfile | null): void {
    load$.next(profile);
    load$.complete();
    fixture.detectChanges();
  }

  function element(selector: string): Element | null {
    return (fixture.nativeElement as HTMLElement).querySelector(selector);
  }

  function documentElements(selector: string): Element[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(selector));
  }

  function click(selector: string): void {
    (element(selector) as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  function setInput(selector: string, value: string): void {
    const input = element(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function valueOf(selector: string): string {
    return (element(selector) as HTMLInputElement).value;
  }

  function submit(): void {
    (element('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const emptyPayload: ProfilePayload = {
    mainTitle: null, targetRoles: [], currentLocation: null, professionalSummary: null, mobility: null,
    desiredLocations: [], workModes: [], contractTypes: [], experiences: [], skills: [], educations: [],
    languages: [], certifications: [], projects: [], additionalInformation: null,
  };

  const existingProfile: CandidateProfile = {
    ...emptyPayload,
    id: 'profile-id',
    mainTitle: 'Backend engineer',
    skills: [{ id: 'skill-id', name: 'Java', category: 'Backend' }],
    experiences: [{
      id: 'experience-id', company: 'Acme', position: 'Engineer', location: null, startDate: '2024-01-01',
      endDate: null, current: true, description: null, achievements: ['Built an API'], skillIds: ['skill-id'],
    }],
  };
});
