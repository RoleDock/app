package io.github.roledock.profile;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profile_experience")
class Experience {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private CandidateProfile profile;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 200)
    private String company;

    @Column(name = "position_title", length = 200)
    private String position;

    @Column(length = 300)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_position", nullable = false)
    private boolean current;

    @Column(length = 5000)
    private String description;

    @ElementCollection
    @OrderColumn(name = "sort_order")
    @JoinTable(name = "experience_achievement", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "achievement", nullable = false, length = 1000)
    private List<String> achievements = new ArrayList<>();

    @ManyToMany
    @OrderColumn(name = "sort_order")
    @JoinTable(
            name = "experience_skill",
            joinColumns = @JoinColumn(name = "experience_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills = new ArrayList<>();

    protected Experience() {
    }

    Experience(UUID id, CandidateProfile profile) {
        this.id = id;
        this.profile = profile;
    }

    UUID getId() {
        return id;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    void update(
            String company,
            String position,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            String description) {
        this.company = company;
        this.position = position;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.current = current;
        this.description = description;
    }

    String getCompany() {
        return company;
    }

    String getPosition() {
        return position;
    }

    String getLocation() {
        return location;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    boolean isCurrent() {
        return current;
    }

    String getDescription() {
        return description;
    }

    List<String> getAchievements() {
        return achievements;
    }

    List<Skill> getSkills() {
        return skills;
    }
}
