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
@Table(name = "profile_certification")
class Certification {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private CandidateProfile profile;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 250)
    private String name;

    @Column(name = "issuing_organization", length = 250)
    private String issuer;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "credential_id", length = 250)
    private String credentialId;

    @Column(name = "credential_url", length = 1000)
    private String credentialUrl;

    protected Certification() {
    }

    Certification(UUID id, CandidateProfile profile) {
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
            String issuer,
            LocalDate issueDate,
            LocalDate expirationDate,
            String credentialId,
            String credentialUrl) {
        this.name = name;
        this.issuer = issuer;
        this.issueDate = issueDate;
        this.expirationDate = expirationDate;
        this.credentialId = credentialId;
        this.credentialUrl = credentialUrl;
    }

    String getName() {
        return name;
    }

    String getIssuer() {
        return issuer;
    }

    LocalDate getIssueDate() {
        return issueDate;
    }

    LocalDate getExpirationDate() {
        return expirationDate;
    }

    String getCredentialId() {
        return credentialId;
    }

    String getCredentialUrl() {
        return credentialUrl;
    }
}
