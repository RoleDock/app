package io.github.roledock.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "profile_skill")
class Skill {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private CandidateProfile profile;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String category;

    protected Skill() {
    }

    Skill(UUID id, CandidateProfile profile) {
        this.id = id;
        this.profile = profile;
    }

    UUID getId() {
        return id;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    void update(String name, String category) {
        this.name = name;
        this.category = category;
    }

    String getName() {
        return name;
    }

    String getCategory() {
        return category;
    }
}
