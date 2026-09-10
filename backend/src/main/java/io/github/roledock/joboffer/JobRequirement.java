package io.github.roledock.joboffer;

import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "job_requirement")
class JobRequirement {
    @Id UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false) JobOffer offer;
    @Column(nullable = false) int sortOrder;
    @Column(nullable = false, columnDefinition = "text") String rawText;
    @Column(nullable = false, columnDefinition = "text") String canonicalLabel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) RequirementCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) RequirementKind requirementKind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Centrality centrality;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Explicitness explicitness;
    @Column(nullable = false) boolean hardBlockerCandidate;
    @Column(columnDefinition = "text") String blockerCondition;
    @Enumerated(EnumType.STRING) @Column(length = 20) ConstraintOperator constraintOperator;
    @Column(columnDefinition = "text") String constraintValue;
    @Column(columnDefinition = "text") String constraintUnit;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) ExtractionConfidence extractionConfidence;

    protected JobRequirement() {}

    JobRequirement(JobOffer offer, int order, Requirement data) {
        id = UUID.randomUUID();
        this.offer = offer;
        sortOrder = order;
        rawText = data.rawText();
        canonicalLabel = data.canonicalLabel();
        category = data.category();
        requirementKind = data.requirementKind();
        centrality = data.centrality();
        explicitness = data.explicitness();
        hardBlockerCandidate = data.hardBlockerCandidate();
        blockerCondition = data.blockerCondition();
        if (data.constraint() != null) {
            constraintOperator = data.constraint().operator();
            constraintValue = data.constraint().value();
            constraintUnit = data.constraint().unit();
        }
        extractionConfidence = data.extractionConfidence();
    }

    Requirement toData() {
        return new Requirement(rawText, canonicalLabel, category, requirementKind, centrality,
                explicitness, hardBlockerCandidate, blockerCondition,
                constraintOperator == null ? null : new Constraint(constraintOperator, constraintValue, constraintUnit),
                extractionConfidence);
    }
}
