import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ProfilePayload } from './profile.models';
import { ProfileService } from './profile.service';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ProfileService, provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProfileService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the current profile from the relative aggregate endpoint', () => {
    service.getCurrent().subscribe((profile) => expect(profile?.mainTitle).toBe('Engineer'));
    const request = httpTesting.expectOne('/api/profile');
    expect(request.request.method).toBe('GET');
    request.flush({ ...emptyPayload, id: 'profile-id', mainTitle: 'Engineer' });
  });

  it('saves the complete payload with PUT on the relative aggregate endpoint', () => {
    service.save(emptyPayload).subscribe((profile) => expect(profile.id).toBe('profile-id'));
    const request = httpTesting.expectOne('/api/profile');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(emptyPayload);
    request.flush({ ...emptyPayload, id: 'profile-id' });
  });

  const emptyPayload: ProfilePayload = {
    mainTitle: null,
    targetRoles: [],
    currentLocation: null,
    professionalSummary: null,
    mobility: null,
    desiredLocations: [],
    workModes: [],
    contractTypes: [],
    experiences: [],
    skills: [],
    educations: [],
    languages: [],
    certifications: [],
    projects: [],
    additionalInformation: null,
  };
});
