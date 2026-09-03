package io.github.roledock.profile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profile")
class CandidateProfile {

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

    UUID getId() { return id; }
    String getProfileKey() { return profileKey; }
    String getMainTitle() { return mainTitle; }
    void setMainTitle(String mainTitle) { this.mainTitle = mainTitle; }
    List<String> getTargetRoles() { return targetRoles; }
    String getCurrentLocation() { return currentLocation; }
    void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    String getProfessionalSummary() { return professionalSummary; }
    void setProfessionalSummary(String professionalSummary) { this.professionalSummary = professionalSummary; }
    String getMobility() { return mobility; }
    void setMobility(String mobility) { this.mobility = mobility; }
    List<String> getDesiredLocations() { return desiredLocations; }
    List<WorkMode> getWorkModes() { return workModes; }
    List<String> getContractTypes() { return contractTypes; }
    List<Experience> getExperiences() { return experiences; }
    List<Skill> getSkills() { return skills; }
    List<Education> getEducations() { return educations; }
    List<Language> getLanguages() { return languages; }
    List<Certification> getCertifications() { return certifications; }
    List<Project> getProjects() { return projects; }
    String getAdditionalInformation() { return additionalInformation; }
    void setAdditionalInformation(String additionalInformation) { this.additionalInformation = additionalInformation; }
}

@Entity
@Table(name = "profile_skill")
class Skill {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 150) private String category;
    protected Skill() { }
    Skill(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String name, String category) { this.name = name; this.category = category; }
    String getName() { return name; }
    String getCategory() { return category; }
}

@Entity
@Table(name = "profile_experience")
class Experience {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(length = 200) private String company;
    @Column(name = "position_title", length = 200) private String position;
    @Column(length = 300) private String location;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "current_position", nullable = false) private boolean current;
    @Column(length = 5000) private String description;
    @ElementCollection @OrderColumn(name = "sort_order")
    @JoinTable(name = "experience_achievement", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "achievement", nullable = false, length = 1000)
    private List<String> achievements = new ArrayList<>();
    @ManyToMany @OrderColumn(name = "sort_order")
    @JoinTable(name = "experience_skill", joinColumns = @JoinColumn(name = "experience_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills = new ArrayList<>();
    protected Experience() { }
    Experience(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String company, String position, String location, LocalDate startDate, LocalDate endDate, boolean current, String description) {
        this.company = company; this.position = position; this.location = location; this.startDate = startDate;
        this.endDate = endDate; this.current = current; this.description = description;
    }
    String getCompany() { return company; }
    String getPosition() { return position; }
    String getLocation() { return location; }
    LocalDate getStartDate() { return startDate; }
    LocalDate getEndDate() { return endDate; }
    boolean isCurrent() { return current; }
    String getDescription() { return description; }
    List<String> getAchievements() { return achievements; }
    List<Skill> getSkills() { return skills; }
}

@Entity
@Table(name = "profile_education")
class Education {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(length = 250) private String institution;
    @Column(name = "degree_title", length = 250) private String degree;
    @Column(name = "field_of_study", length = 200) private String field;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(length = 3000) private String description;
    protected Education() { }
    Education(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String institution, String degree, String field, LocalDate startDate, LocalDate endDate, String description) {
        this.institution = institution; this.degree = degree; this.field = field; this.startDate = startDate; this.endDate = endDate; this.description = description;
    }
    String getInstitution() { return institution; } String getDegree() { return degree; } String getField() { return field; }
    LocalDate getStartDate() { return startDate; } LocalDate getEndDate() { return endDate; } String getDescription() { return description; }
}

@Entity
@Table(name = "profile_language")
class Language {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(length = 150) private String name;
    @Column(name = "declared_level", length = 150) private String level;
    protected Language() { }
    Language(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; } void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String name, String level) { this.name = name; this.level = level; }
    String getName() { return name; } String getLevel() { return level; }
}

@Entity
@Table(name = "profile_certification")
class Certification {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(length = 250) private String name;
    @Column(name = "issuing_organization", length = 250) private String issuer;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "expiration_date") private LocalDate expirationDate;
    @Column(name = "credential_id", length = 250) private String credentialId;
    @Column(name = "credential_url", length = 1000) private String credentialUrl;
    protected Certification() { }
    Certification(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String name, String issuer, LocalDate issueDate, LocalDate expirationDate, String credentialId, String credentialUrl) {
        this.name = name; this.issuer = issuer; this.issueDate = issueDate; this.expirationDate = expirationDate; this.credentialId = credentialId; this.credentialUrl = credentialUrl;
    }
    String getName() { return name; } String getIssuer() { return issuer; } LocalDate getIssueDate() { return issueDate; }
    LocalDate getExpirationDate() { return expirationDate; } String getCredentialId() { return credentialId; } String getCredentialUrl() { return credentialUrl; }
}

@Entity
@Table(name = "profile_project")
class Project {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private CandidateProfile profile;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(length = 250) private String name;
    @Column(name = "project_role", length = 200) private String role;
    @Column(length = 3000) private String description;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "project_url", length = 1000) private String url;
    protected Project() { }
    Project(UUID id, CandidateProfile profile) { this.id = id; this.profile = profile; }
    UUID getId() { return id; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    void update(String name, String role, String description, LocalDate startDate, LocalDate endDate, String url) {
        this.name = name; this.role = role; this.description = description; this.startDate = startDate; this.endDate = endDate; this.url = url;
    }
    String getName() { return name; } String getRole() { return role; } String getDescription() { return description; }
    LocalDate getStartDate() { return startDate; } LocalDate getEndDate() { return endDate; } String getUrl() { return url; }
}
