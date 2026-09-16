package io.github.roledock.profile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profile")
class CandidateProfile {
    @Column(name = "certifications_complete", nullable = false)
    private boolean certificationsComplete;

    boolean isCertificationsComplete() { return certificationsComplete; }
    void setCertificationsComplete(boolean complete) { certificationsComplete = complete; }

    @Id
    private UUID id;

    @Column(name = "profile_key", nullable = false, length = 20)
    private String profileKey = "CURRENT";

    @Column(name = "main_title", length = 200)
    private String mainTitle;

    @ElementCollection
    @OrderColumn(name = "sort_order")
    @JoinTable(name = "profile_target_role", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "role_name", nullable = false, length = 200)
    private List<String> targetRoles = new ArrayList<>();

    @Column(name = "current_location", length = 300)
    private String currentLocation;

    @Column(name = "professional_summary", length = 5000)
    private String professionalSummary;

    @Column(length = 500)
    private String mobility;

    @ElementCollection
    @OrderColumn(name = "sort_order")
    @JoinTable(name = "profile_desired_location", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "location_name", nullable = false, length = 300)
    private List<String> desiredLocations = new ArrayList<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @OrderColumn(name = "sort_order")
    @JoinTable(name = "profile_work_mode", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "work_mode", nullable = false, length = 20)
    private List<WorkMode> workModes = new ArrayList<>();

    @ElementCollection
    @OrderColumn(name = "sort_order")
    @JoinTable(name = "profile_contract_type", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "contract_type", nullable = false, length = 100)
    private List<String> contractTypes = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Experience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Language> languages = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Certification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Project> projects = new ArrayList<>();

    @Column(name = "additional_information", length = 5000)
    private String additionalInformation;

    protected CandidateProfile() {
    }

    CandidateProfile(UUID id) {
        this.id = id;
    }

    UUID getId() {
        return id;
    }

    String getProfileKey() {
        return profileKey;
    }

    String getMainTitle() {
        return mainTitle;
    }

    void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    List<String> getTargetRoles() {
        return targetRoles;
    }

    String getCurrentLocation() {
        return currentLocation;
    }

    void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    String getProfessionalSummary() {
        return professionalSummary;
    }

    void setProfessionalSummary(String professionalSummary) {
        this.professionalSummary = professionalSummary;
    }

    String getMobility() {
        return mobility;
    }

    void setMobility(String mobility) {
        this.mobility = mobility;
    }

    List<String> getDesiredLocations() {
        return desiredLocations;
    }

    List<WorkMode> getWorkModes() {
        return workModes;
    }

    List<String> getContractTypes() {
        return contractTypes;
    }

    List<Experience> getExperiences() {
        return experiences;
    }

    List<Skill> getSkills() {
        return skills;
    }

    List<Education> getEducations() {
        return educations;
    }

    List<Language> getLanguages() {
        return languages;
    }

    List<Certification> getCertifications() {
        return certifications;
    }

    List<Project> getProjects() {
        return projects;
    }

    String getAdditionalInformation() {
        return additionalInformation;
    }

    void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}
