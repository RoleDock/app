package io.github.roledock.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record SaveRequest(
            @Size(max = 200) String mainTitle,
            List<@Size(max = 200) String> targetRoles,
            @Size(max = 300) String currentLocation,
            @Size(max = 5000) String professionalSummary,
            @Size(max = 500) String mobility,
            List<@Size(max = 300) String> desiredLocations,
            List<@NotNull WorkMode> workModes,
            List<@Size(max = 100) String> contractTypes,
            List<@NotNull @Valid ExperienceData> experiences,
            List<@NotNull @Valid SkillData> skills,
            List<@NotNull @Valid EducationData> educations,
            List<@NotNull @Valid LanguageData> languages,
            List<@NotNull @Valid CertificationData> certifications,
            List<@NotNull @Valid ProjectData> projects,
            @Size(max = 5000) String additionalInformation) {
    }

    public record Response(
            UUID id,
            String mainTitle,
            List<String> targetRoles,
            String currentLocation,
            String professionalSummary,
            String mobility,
            List<String> desiredLocations,
            List<WorkMode> workModes,
            List<String> contractTypes,
            List<ExperienceData> experiences,
            List<SkillData> skills,
            List<EducationData> educations,
            List<LanguageData> languages,
            List<CertificationData> certifications,
            List<ProjectData> projects,
            String additionalInformation) {
    }

    public record ExperienceData(
            @NotNull UUID id,
            @Size(max = 200) String company,
            @Size(max = 200) String position,
            @Size(max = 300) String location,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            @Size(max = 5000) String description,
            List<@Size(max = 1000) String> achievements,
            List<@NotNull UUID> skillIds) {
    }

    public record SkillData(
            @NotNull UUID id,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 150) String category) {
    }

    public record EducationData(
            @NotNull UUID id,
            @Size(max = 250) String institution,
            @Size(max = 250) String degree,
            @Size(max = 200) String field,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 3000) String description) {
    }

    public record LanguageData(
            @NotNull UUID id,
            @Size(max = 150) String name,
            @Size(max = 150) String level) {
    }

    public record CertificationData(
            @NotNull UUID id,
            @Size(max = 250) String name,
            @Size(max = 250) String issuer,
            LocalDate issueDate,
            LocalDate expirationDate,
            @Size(max = 250) String credentialId,
            @Size(max = 1000) String credentialUrl) {
    }

    public record ProjectData(
            @NotNull UUID id,
            @Size(max = 250) String name,
            @Size(max = 200) String role,
            @Size(max = 3000) String description,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 1000) String url) {
    }
}
