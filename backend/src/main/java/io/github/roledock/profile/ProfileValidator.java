package io.github.roledock.profile;

import static io.github.roledock.profile.ProfileDtos.CertificationData;
import static io.github.roledock.profile.ProfileDtos.EducationData;
import static io.github.roledock.profile.ProfileDtos.ExperienceData;
import static io.github.roledock.profile.ProfileDtos.LanguageData;
import static io.github.roledock.profile.ProfileDtos.ProjectData;
import static io.github.roledock.profile.ProfileDtos.SaveRequest;
import static io.github.roledock.profile.ProfileDtos.SkillData;

import io.github.roledock.api.error.ApiValidationException;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class ProfileValidator {

    private static final String INVALID_PROFILE_MESSAGE = "Le profil contient des données invalides.";

    private final EntityManager entityManager;

    ProfileValidator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    void validate(SaveRequest request, CandidateProfile currentProfile) {
        Map<String, String> errors = new LinkedHashMap<>();

        checkUniqueIds(safe(request.skills()).stream().map(SkillData::id).toList(), "skills", errors);
        checkUniqueIds(safe(request.experiences()).stream().map(ExperienceData::id).toList(), "experiences", errors);
        checkUniqueIds(safe(request.educations()).stream().map(EducationData::id).toList(), "educations", errors);
        checkUniqueIds(safe(request.languages()).stream().map(LanguageData::id).toList(), "languages", errors);
        checkUniqueIds(safe(request.certifications()).stream().map(CertificationData::id).toList(), "certifications", errors);
        checkUniqueIds(safe(request.projects()).stream().map(ProjectData::id).toList(), "projects", errors);

        validateExperienceRules(request, errors);
        validateDateAndUrlRules(request, errors);
        validateOwnership(request, currentProfile, errors);

        if (!errors.isEmpty()) {
            throw new ApiValidationException(INVALID_PROFILE_MESSAGE, errors);
        }
    }

    private void validateExperienceRules(SaveRequest request, Map<String, String> errors) {
        Set<UUID> profileSkillIds = new HashSet<>(safe(request.skills()).stream().map(SkillData::id).toList());
        List<ExperienceData> experiences = safe(request.experiences());

        for (int i = 0; i < experiences.size(); i++) {
            ExperienceData experience = experiences.get(i);
            String path = "experiences[" + i + "]";
            checkDates(experience.startDate(), experience.endDate(), path, errors);
            if (experience.current() && experience.endDate() != null) {
                errors.put(path + ".endDate", "Une expérience actuelle ne peut pas avoir de date de fin.");
            }

            Set<UUID> attachedSkillIds = new HashSet<>();
            for (UUID skillId : safe(experience.skillIds())) {
                if (!profileSkillIds.contains(skillId)) {
                    errors.put(path + ".skillIds", "Chaque compétence rattachée doit appartenir au profil envoyé.");
                }
                if (!attachedSkillIds.add(skillId)) {
                    errors.put(path + ".skillIds", "Une compétence ne peut être rattachée qu'une fois.");
                }
            }
        }
    }

    private void validateDateAndUrlRules(SaveRequest request, Map<String, String> errors) {
        List<EducationData> educations = safe(request.educations());
        for (int i = 0; i < educations.size(); i++) {
            EducationData education = educations.get(i);
            checkDates(education.startDate(), education.endDate(), "educations[" + i + "]", errors);
        }

        List<CertificationData> certifications = safe(request.certifications());
        for (int i = 0; i < certifications.size(); i++) {
            CertificationData certification = certifications.get(i);
            String path = "certifications[" + i + "]";
            checkDates(certification.issueDate(), certification.expirationDate(), path, errors);
            checkUrl(certification.credentialUrl(), path + ".credentialUrl", errors);
        }

        List<ProjectData> projects = safe(request.projects());
        for (int i = 0; i < projects.size(); i++) {
            ProjectData project = projects.get(i);
            String path = "projects[" + i + "]";
            checkDates(project.startDate(), project.endDate(), path, errors);
            checkUrl(project.url(), path + ".url", errors);
        }
    }

    private void validateOwnership(
            SaveRequest request,
            CandidateProfile currentProfile,
            Map<String, String> errors) {
        checkOwnership(
                safe(request.skills()).stream().map(SkillData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getSkills, Skill::getId),
                Skill.class,
                "skills",
                errors);
        checkOwnership(
                safe(request.experiences()).stream().map(ExperienceData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getExperiences, Experience::getId),
                Experience.class,
                "experiences",
                errors);
        checkOwnership(
                safe(request.educations()).stream().map(EducationData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getEducations, Education::getId),
                Education.class,
                "educations",
                errors);
        checkOwnership(
                safe(request.languages()).stream().map(LanguageData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getLanguages, Language::getId),
                Language.class,
                "languages",
                errors);
        checkOwnership(
                safe(request.certifications()).stream().map(CertificationData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getCertifications, Certification::getId),
                Certification.class,
                "certifications",
                errors);
        checkOwnership(
                safe(request.projects()).stream().map(ProjectData::id).toList(),
                ownedIds(currentProfile, CandidateProfile::getProjects, Project::getId),
                Project.class,
                "projects",
                errors);
    }

    private static <E> Set<UUID> ownedIds(
            CandidateProfile currentProfile,
            Function<CandidateProfile, List<E>> ownedEntities,
            Function<E, UUID> entityId) {
        if (currentProfile == null) {
            return Set.of();
        }
        return ownedEntities.apply(currentProfile).stream()
                .map(entityId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private <E> void checkOwnership(
            List<UUID> inputIds,
            Set<UUID> ownedIds,
            Class<E> entityType,
            String field,
            Map<String, String> errors) {
        for (UUID inputId : inputIds) {
            if (inputId != null && !ownedIds.contains(inputId) && entityManager.find(entityType, inputId) != null) {
                errors.put(field, "Un identifiant appartient à un autre profil.");
            }
        }
    }

    private static void checkDates(LocalDate start, LocalDate end, String path, Map<String, String> errors) {
        if (start != null && end != null && end.isBefore(start)) {
            errors.put(path + ".endDate", "La date de fin ne peut pas précéder la date de début.");
        }
    }

    private static void checkUrl(String value, String path, Map<String, String> errors) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return;
        }
        try {
            URI uri = URI.create(cleaned);
            boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!http || uri.getHost() == null) {
                errors.put(path, "L'URL doit être une adresse HTTP ou HTTPS valide.");
            }
        } catch (IllegalArgumentException exception) {
            errors.put(path, "L'URL doit être une adresse HTTP ou HTTPS valide.");
        }
    }

    private static void checkUniqueIds(List<UUID> ids, String field, Map<String, String> errors) {
        if (ids.stream().anyMatch(java.util.Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            errors.put(field, "Les identifiants doivent être présents et uniques.");
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
