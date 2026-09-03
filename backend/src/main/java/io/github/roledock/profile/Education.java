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
@Table(name = "profile_education")
class Education {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private CandidateProfile profile;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 250)
    private String institution;

    @Column(name = "degree_title", length = 250)
    private String degree;

    @Column(name = "field_of_study", length = 200)
    private String field;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 3000)
    private String description;

    protected Education() {
    }

    Education(UUID id, CandidateProfile profile) {
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
            String institution,
            String degree,
            String field,
            LocalDate startDate,
            LocalDate endDate,
            String description) {
        this.institution = institution;
        this.degree = degree;
        this.field = field;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    String getInstitution() {
        return institution;
    }

    String getDegree() {
        return degree;
    }

    String getField() {
        return field;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    String getDescription() {
        return description;
    }
}
