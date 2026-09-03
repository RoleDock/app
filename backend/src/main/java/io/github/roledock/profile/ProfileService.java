package io.github.roledock.profile;

import static io.github.roledock.profile.ProfileDtos.*;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProfileService {
    private static final String CURRENT = "CURRENT";

    private final CandidateProfileRepository repository;
    private final EntityManager entityManager;
    private final ProfileValidator validator;

    ProfileService(CandidateProfileRepository repository, EntityManager entityManager, ProfileValidator validator) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    Optional<Response> getCurrentProfile() {
        return repository.findByProfileKey(CURRENT).map(this::toResponse);
    }

    @Transactional
    Response saveCurrentProfile(SaveRequest request) {
        Optional<CandidateProfile> currentProfile = repository.findByProfileKey(CURRENT);
        validator.validate(request, currentProfile.orElse(null));
        CandidateProfile profile = currentProfile.orElseGet(() -> new CandidateProfile(UUID.randomUUID()));

        profile.setMainTitle(clean(request.mainTitle()));
        replaceStrings(profile.getTargetRoles(), request.targetRoles());
        profile.setCurrentLocation(clean(request.currentLocation()));
        profile.setProfessionalSummary(clean(request.professionalSummary()));
        profile.setMobility(clean(request.mobility()));
        replaceStrings(profile.getDesiredLocations(), request.desiredLocations());
        replaceValues(profile.getWorkModes(), safe(request.workModes()));
        replaceStrings(profile.getContractTypes(), request.contractTypes());
        profile.setAdditionalInformation(clean(request.additionalInformation()));

        Map<UUID, Skill> skills = syncSkills(profile, safe(request.skills()));
        syncExperiences(profile, safe(request.experiences()), skills);
        syncEducations(profile, safe(request.educations()));
        syncLanguages(profile, safe(request.languages()));
        syncCertifications(profile, safe(request.certifications()));
        syncProjects(profile, safe(request.projects()));

        if (currentProfile.isEmpty()) {
            entityManager.persist(profile);
        }
        entityManager.flush();
        return toResponse(profile);
    }

    private Map<UUID, Skill> syncSkills(CandidateProfile profile, List<SkillData> inputs) {
        Map<UUID, Skill> existing = index(profile.getSkills(), Skill::getId);
        List<Skill> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            SkillData input = inputs.get(i);
            Skill skill = existing.computeIfAbsent(input.id(), id -> new Skill(id, profile));
            skill.setSortOrder(i);
            skill.update(cleanRequired(input.name()), clean(input.category()));
            updated.add(skill);
        }
        replaceValues(profile.getSkills(), updated);
        return index(updated, Skill::getId);
    }

    private void syncExperiences(CandidateProfile profile, List<ExperienceData> inputs, Map<UUID, Skill> skills) {
        Map<UUID, Experience> existing = index(profile.getExperiences(), Experience::getId);
        List<Experience> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            ExperienceData input = inputs.get(i);
            Experience experience = existing.computeIfAbsent(input.id(), id -> new Experience(id, profile));
            experience.setSortOrder(i);
            experience.update(clean(input.company()), clean(input.position()), clean(input.location()), input.startDate(),
                    input.endDate(), input.current(), clean(input.description()));
            replaceStrings(experience.getAchievements(), input.achievements());
            List<Skill> attached = safe(input.skillIds()).stream().map(skills::get).toList();
            replaceValues(experience.getSkills(), attached);
            updated.add(experience);
        }
        replaceValues(profile.getExperiences(), updated);
    }

    private void syncEducations(CandidateProfile profile, List<EducationData> inputs) {
        Map<UUID, Education> existing = index(profile.getEducations(), Education::getId);
        List<Education> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            EducationData input = inputs.get(i);
            Education education = existing.computeIfAbsent(input.id(), id -> new Education(id, profile));
            education.setSortOrder(i);
            education.update(clean(input.institution()), clean(input.degree()), clean(input.field()), input.startDate(), input.endDate(), clean(input.description()));
            updated.add(education);
        }
        replaceValues(profile.getEducations(), updated);
    }

    private void syncLanguages(CandidateProfile profile, List<LanguageData> inputs) {
        Map<UUID, Language> existing = index(profile.getLanguages(), Language::getId);
        List<Language> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            LanguageData input = inputs.get(i);
            Language language = existing.computeIfAbsent(input.id(), id -> new Language(id, profile));
            language.setSortOrder(i);
            language.update(clean(input.name()), clean(input.level()));
            updated.add(language);
        }
        replaceValues(profile.getLanguages(), updated);
    }

    private void syncCertifications(CandidateProfile profile, List<CertificationData> inputs) {
        Map<UUID, Certification> existing = index(profile.getCertifications(), Certification::getId);
        List<Certification> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            CertificationData input = inputs.get(i);
            Certification certification = existing.computeIfAbsent(input.id(), id -> new Certification(id, profile));
            certification.setSortOrder(i);
            certification.update(clean(input.name()), clean(input.issuer()), input.issueDate(), input.expirationDate(),
                    clean(input.credentialId()), clean(input.credentialUrl()));
            updated.add(certification);
        }
        replaceValues(profile.getCertifications(), updated);
    }

    private void syncProjects(CandidateProfile profile, List<ProjectData> inputs) {
        Map<UUID, Project> existing = index(profile.getProjects(), Project::getId);
        List<Project> updated = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            ProjectData input = inputs.get(i);
            Project project = existing.computeIfAbsent(input.id(), id -> new Project(id, profile));
            project.setSortOrder(i);
            project.update(clean(input.name()), clean(input.role()), clean(input.description()), input.startDate(), input.endDate(), clean(input.url()));
            updated.add(project);
        }
        replaceValues(profile.getProjects(), updated);
    }

    private Response toResponse(CandidateProfile profile) {
        return new Response(profile.getId(), profile.getMainTitle(), List.copyOf(profile.getTargetRoles()),
                profile.getCurrentLocation(), profile.getProfessionalSummary(), profile.getMobility(),
                List.copyOf(profile.getDesiredLocations()), List.copyOf(profile.getWorkModes()), List.copyOf(profile.getContractTypes()),
                profile.getExperiences().stream().map(e -> new ExperienceData(e.getId(), e.getCompany(), e.getPosition(), e.getLocation(),
                        e.getStartDate(), e.getEndDate(), e.isCurrent(), e.getDescription(), List.copyOf(e.getAchievements()),
                        e.getSkills().stream().map(Skill::getId).toList())).toList(),
                profile.getSkills().stream().map(s -> new SkillData(s.getId(), s.getName(), s.getCategory())).toList(),
                profile.getEducations().stream().map(e -> new EducationData(e.getId(), e.getInstitution(), e.getDegree(), e.getField(), e.getStartDate(), e.getEndDate(), e.getDescription())).toList(),
                profile.getLanguages().stream().map(l -> new LanguageData(l.getId(), l.getName(), l.getLevel())).toList(),
                profile.getCertifications().stream().map(c -> new CertificationData(c.getId(), c.getName(), c.getIssuer(), c.getIssueDate(), c.getExpirationDate(), c.getCredentialId(), c.getCredentialUrl())).toList(),
                profile.getProjects().stream().map(p -> new ProjectData(p.getId(), p.getName(), p.getRole(), p.getDescription(), p.getStartDate(), p.getEndDate(), p.getUrl())).toList(),
                profile.getAdditionalInformation());
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String cleanRequired(String value) {
        return value == null ? null : value.strip();
    }

    private static void replaceStrings(List<String> target, List<String> source) {
        replaceValues(target, safe(source).stream().map(ProfileService::clean).filter(java.util.Objects::nonNull).toList());
    }

    private static <T> void replaceValues(List<T> target, List<T> source) {
        target.clear();
        target.addAll(source);
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> Map<UUID, T> index(List<T> values, Function<T, UUID> id) {
        Map<UUID, T> indexed = new HashMap<>();
        values.forEach(value -> indexed.put(id.apply(value), value));
        return indexed;
    }
}
