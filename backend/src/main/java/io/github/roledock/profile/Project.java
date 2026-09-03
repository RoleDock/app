package io.github.roledock.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "profile_project")
class Project {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private CandidateProfile profile;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 250)
    private String name;

    @Column(name = "project_role", length = 200)
    private String role;

    @Column(length = 3000)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "project_url", length = 1000)
    private String url;

    protected Project() {
    }

    Project(UUID id, CandidateProfile profile) {
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
            String name,
            String role,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String url) {
        this.name = name;
        this.role = role;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.url = url;
    }

    String getName() {
        return name;
    }

    String getRole() {
        return role;
    }

    String getDescription() {
        return description;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    String getUrl() {
        return url;
    }
}
