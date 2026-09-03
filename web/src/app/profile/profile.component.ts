import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { RouterLink } from '@angular/router';

import {
  CandidateProfile,
  CertificationData,
  EducationData,
  ExperienceData,
  LanguageData,
  ProfilePayload,
  ProjectData,
  SkillData,
  WorkMode,
} from './profile.models';
import { ProfileService } from './profile.service';

type TextControl = FormControl<string>;

type ExperienceForm = FormGroup<{
  id: TextControl;
  company: TextControl;
  position: TextControl;
  location: TextControl;
  startDate: TextControl;
  endDate: TextControl;
  current: FormControl<boolean>;
  description: TextControl;
  achievements: FormArray<TextControl>;
  skillIds: FormArray<TextControl>;
}>;

type SkillForm = FormGroup<{ id: TextControl; name: TextControl; category: TextControl }>;
type EducationForm = FormGroup<{
  id: TextControl;
  institution: TextControl;
  degree: TextControl;
  field: TextControl;
  startDate: TextControl;
  endDate: TextControl;
  description: TextControl;
}>;
type LanguageForm = FormGroup<{ id: TextControl; name: TextControl; level: TextControl }>;
type CertificationForm = FormGroup<{
  id: TextControl;
  name: TextControl;
  issuer: TextControl;
  issueDate: TextControl;
  expirationDate: TextControl;
  credentialId: TextControl;
  credentialUrl: TextControl;
}>;
type ProjectForm = FormGroup<{
  id: TextControl;
  name: TextControl;
  role: TextControl;
  description: TextControl;
  startDate: TextControl;
  endDate: TextControl;
  url: TextControl;
}>;

const dateRangeValidator = (startField: string, endField: string, currentField?: string): ValidatorFn =>
  (control: AbstractControl): ValidationErrors | null => {
    const start = control.get(startField)?.value as string | undefined;
    const end = control.get(endField)?.value as string | undefined;
    const current = currentField ? Boolean(control.get(currentField)?.value) : false;
    if (current && end) return { currentWithEndDate: true };
    return start && end && end < start ? { dateRange: true } : null;
  };

const text = (value: string | null | undefined = '', maxLength?: number, required = false): TextControl => {
  const validators = [maxLength ? Validators.maxLength(maxLength) : null, required ? Validators.required : null].filter(
    (validator): validator is ValidatorFn => validator !== null,
  );
  return new FormControl(value ?? '', { nonNullable: true, validators });
};

const urlText = (value: string | null | undefined): TextControl => {
  const control = text(value, 1000);
  control.addValidators(Validators.pattern(/^https?:\/\/[^\s]+$/i));
  return control;
};

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly profileExists = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveSuccess = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly submitted = signal(false);
  protected readonly workModeOptions: ReadonlyArray<{ value: WorkMode; label: string }> = [
    { value: 'ONSITE', label: 'Présentiel' },
    { value: 'HYBRID', label: 'Hybride' },
    { value: 'REMOTE', label: 'Télétravail' },
  ];

  protected readonly form = new FormGroup({
    mainTitle: text('', 200),
    targetRoles: new FormArray<TextControl>([]),
    currentLocation: text('', 300),
    professionalSummary: text('', 5000),
    mobility: text('', 500),
    desiredLocations: new FormArray<TextControl>([]),
    workModes: new FormGroup({
      ONSITE: new FormControl(false, { nonNullable: true }),
      HYBRID: new FormControl(false, { nonNullable: true }),
      REMOTE: new FormControl(false, { nonNullable: true }),
    }),
    contractTypes: new FormArray<TextControl>([]),
    experiences: new FormArray<ExperienceForm>([]),
    skills: new FormArray<SkillForm>([]),
    educations: new FormArray<EducationForm>([]),
    languages: new FormArray<LanguageForm>([]),
    certifications: new FormArray<CertificationForm>([]),
    projects: new FormArray<ProjectForm>([]),
    additionalInformation: text('', 5000),
  });

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.profileService
      .getCurrent()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (profile) => {
          this.populate(profile);
          this.profileExists.set(profile !== null);
        },
        error: () => this.loadError.set(true),
      });
  }

  protected save(): void {
    this.submitted.set(true);
    this.saveSuccess.set(false);
    this.saveError.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.saving.set(true);
    this.profileService
      .save(this.toPayload())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (profile) => {
          this.populate(profile);
          this.profileExists.set(true);
          this.submitted.set(false);
          this.saveSuccess.set(true);
        },
        error: () => this.saveError.set('Enregistrement impossible. Votre saisie est conservée ; vous pouvez réessayer.'),
      });
  }

  protected addTextItem(array: FormArray<TextControl>, maxLength: number): void {
    array.push(text('', maxLength));
  }

  protected removeTextItem(array: FormArray<TextControl>, index: number): void {
    array.removeAt(index);
  }

  protected addSkill(): void { this.form.controls.skills.push(this.createSkill()); }
  protected addExperience(): void { this.form.controls.experiences.push(this.createExperience()); }
  protected addEducation(): void { this.form.controls.educations.push(this.createEducation()); }
  protected addLanguage(): void { this.form.controls.languages.push(this.createLanguage()); }
  protected addCertification(): void { this.form.controls.certifications.push(this.createCertification()); }
  protected addProject(): void { this.form.controls.projects.push(this.createProject()); }

  protected removeSkill(index: number): void {
    const id = this.form.controls.skills.at(index).controls.id.value;
    this.form.controls.skills.removeAt(index);
    this.form.controls.experiences.controls.forEach((experience) => {
      const skillIndex = experience.controls.skillIds.controls.findIndex((skill) => skill.value === id);
      if (skillIndex >= 0) experience.controls.skillIds.removeAt(skillIndex);
    });
  }

  protected toggleExperienceSkill(experience: ExperienceForm, skillId: string, checked: boolean): void {
    const selected = experience.controls.skillIds;
    const index = selected.controls.findIndex((control) => control.value === skillId);
    if (checked && index < 0) selected.push(text(skillId));
    if (!checked && index >= 0) selected.removeAt(index);
    selected.markAsDirty();
  }

  protected isSkillAttached(experience: ExperienceForm, skillId: string): boolean {
    return experience.controls.skillIds.controls.some((control) => control.value === skillId);
  }

  protected trackControl(_index: number, control: AbstractControl): AbstractControl {
    return control;
  }

  private createExperience(value?: ExperienceData): ExperienceForm {
    return new FormGroup(
      {
        id: text(value?.id ?? this.newId()),
        company: text(value?.company, 200),
        position: text(value?.position, 200),
        location: text(value?.location, 300),
        startDate: text(value?.startDate),
        endDate: text(value?.endDate),
        current: new FormControl(value?.current ?? false, { nonNullable: true }),
        description: text(value?.description, 5000),
        achievements: new FormArray((value?.achievements ?? []).map((item) => text(item, 1000))),
        skillIds: new FormArray((value?.skillIds ?? []).map((id) => text(id))),
      },
      { validators: dateRangeValidator('startDate', 'endDate', 'current') },
    );
  }

  private createSkill(value?: SkillData): SkillForm {
    return new FormGroup({
      id: text(value?.id ?? this.newId()),
      name: text(value?.name, 150, true),
      category: text(value?.category, 150),
    });
  }

  private createEducation(value?: EducationData): EducationForm {
    return new FormGroup(
      {
        id: text(value?.id ?? this.newId()), institution: text(value?.institution, 250), degree: text(value?.degree, 250),
        field: text(value?.field, 200), startDate: text(value?.startDate), endDate: text(value?.endDate),
        description: text(value?.description, 3000),
      },
      { validators: dateRangeValidator('startDate', 'endDate') },
    );
  }

  private createLanguage(value?: LanguageData): LanguageForm {
    return new FormGroup({ id: text(value?.id ?? this.newId()), name: text(value?.name, 150), level: text(value?.level, 150) });
  }

  private createCertification(value?: CertificationData): CertificationForm {
    return new FormGroup(
      {
        id: text(value?.id ?? this.newId()), name: text(value?.name, 250), issuer: text(value?.issuer, 250),
        issueDate: text(value?.issueDate), expirationDate: text(value?.expirationDate), credentialId: text(value?.credentialId, 250),
        credentialUrl: urlText(value?.credentialUrl),
      },
      { validators: dateRangeValidator('issueDate', 'expirationDate') },
    );
  }

  private createProject(value?: ProjectData): ProjectForm {
    return new FormGroup(
      {
        id: text(value?.id ?? this.newId()), name: text(value?.name, 250), role: text(value?.role, 200),
        description: text(value?.description, 3000), startDate: text(value?.startDate), endDate: text(value?.endDate), url: urlText(value?.url),
      },
      { validators: dateRangeValidator('startDate', 'endDate') },
    );
  }

  private populate(profile: CandidateProfile | null): void {
    const value = profile ?? this.emptyProfile();
    this.form.patchValue({
      mainTitle: value.mainTitle ?? '', currentLocation: value.currentLocation ?? '', professionalSummary: value.professionalSummary ?? '',
      mobility: value.mobility ?? '', additionalInformation: value.additionalInformation ?? '',
      workModes: {
        ONSITE: value.workModes.includes('ONSITE'), HYBRID: value.workModes.includes('HYBRID'), REMOTE: value.workModes.includes('REMOTE'),
      },
    });
    this.replaceArray(this.form.controls.targetRoles, value.targetRoles.map((item) => text(item, 200)));
    this.replaceArray(this.form.controls.desiredLocations, value.desiredLocations.map((item) => text(item, 300)));
    this.replaceArray(this.form.controls.contractTypes, value.contractTypes.map((item) => text(item, 100)));
    this.replaceArray(this.form.controls.experiences, value.experiences.map((item) => this.createExperience(item)));
    this.replaceArray(this.form.controls.skills, value.skills.map((item) => this.createSkill(item)));
    this.replaceArray(this.form.controls.educations, value.educations.map((item) => this.createEducation(item)));
    this.replaceArray(this.form.controls.languages, value.languages.map((item) => this.createLanguage(item)));
    this.replaceArray(this.form.controls.certifications, value.certifications.map((item) => this.createCertification(item)));
    this.replaceArray(this.form.controls.projects, value.projects.map((item) => this.createProject(item)));
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  private toPayload(): ProfilePayload {
    const raw = this.form.getRawValue();
    return {
      mainTitle: this.nullable(raw.mainTitle),
      targetRoles: this.nonEmpty(raw.targetRoles),
      currentLocation: this.nullable(raw.currentLocation),
      professionalSummary: this.nullable(raw.professionalSummary),
      mobility: this.nullable(raw.mobility),
      desiredLocations: this.nonEmpty(raw.desiredLocations),
      workModes: this.workModeOptions.filter(({ value }) => raw.workModes[value]).map(({ value }) => value),
      contractTypes: this.nonEmpty(raw.contractTypes),
      experiences: raw.experiences.map((item) => ({
        ...item,
        company: this.nullable(item.company), position: this.nullable(item.position), location: this.nullable(item.location),
        startDate: this.nullable(item.startDate), endDate: this.nullable(item.endDate), description: this.nullable(item.description),
        achievements: this.nonEmpty(item.achievements),
      })),
      skills: raw.skills.map((item) => ({ id: item.id, name: item.name.trim(), category: this.nullable(item.category) })),
      educations: raw.educations.map((item) => ({
        ...item, institution: this.nullable(item.institution), degree: this.nullable(item.degree), field: this.nullable(item.field),
        startDate: this.nullable(item.startDate), endDate: this.nullable(item.endDate), description: this.nullable(item.description),
      })),
      languages: raw.languages.map((item) => ({ id: item.id, name: this.nullable(item.name), level: this.nullable(item.level) })),
      certifications: raw.certifications.map((item) => ({
        ...item, name: this.nullable(item.name), issuer: this.nullable(item.issuer), issueDate: this.nullable(item.issueDate),
        expirationDate: this.nullable(item.expirationDate), credentialId: this.nullable(item.credentialId), credentialUrl: this.nullable(item.credentialUrl),
      })),
      projects: raw.projects.map((item) => ({
        ...item, name: this.nullable(item.name), role: this.nullable(item.role), description: this.nullable(item.description),
        startDate: this.nullable(item.startDate), endDate: this.nullable(item.endDate), url: this.nullable(item.url),
      })),
      additionalInformation: this.nullable(raw.additionalInformation),
    };
  }

  private emptyProfile(): ProfilePayload {
    return {
      mainTitle: null, targetRoles: [], currentLocation: null, professionalSummary: null, mobility: null,
      desiredLocations: [], workModes: [], contractTypes: [], experiences: [], skills: [], educations: [], languages: [],
      certifications: [], projects: [], additionalInformation: null,
    };
  }

  private replaceArray<T extends AbstractControl>(target: FormArray<T>, values: T[]): void {
    target.clear();
    values.forEach((value) => target.push(value));
  }

  private nullable(value: string): string | null {
    const cleaned = value.trim();
    return cleaned === '' ? null : cleaned;
  }

  private nonEmpty(values: string[]): string[] {
    return values.map((value) => value.trim()).filter(Boolean);
  }

  private newId(): string {
    return globalThis.crypto.randomUUID();
  }
}
