package io.github.roledock.joboffer;

import io.github.roledock.joboffer.extraction.JobOfferExtraction;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "job_offer")
class JobOffer {
    @Id UUID id;
    @Column(nullable = false, columnDefinition = "text", updatable = false) String originalText;
    @Column(length = 2000) String sourceUrl;
    @Column(nullable = false, updatable = false) Instant analyzedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    ReviewStatus reviewStatus = ReviewStatus.UNREVIEWED;
    Instant reviewBypassedAt;
    // Only the validated public contract; never a provider response or diagnostic.
    @Column(nullable = false, columnDefinition = "text", updatable = false) String initialExtraction;
    @Column(columnDefinition = "text") String company;
    @Column(columnDefinition = "text") String position;
    @Column(columnDefinition = "text") String sourceLanguage;
    @Column(columnDefinition = "text") String summary;
    @Column(columnDefinition = "text") String city;
    @Column(columnDefinition = "text") String region;
    @Column(columnDefinition = "text") String country;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) ContractType contractType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) WorkArrangementType workArrangementType;
    @Column(columnDefinition = "text") String remoteArea;
    Integer onSiteDaysPerWeek;
    @ElementCollection
    @CollectionTable(name = "job_offer_mission", joinColumns = @JoinColumn(name = "job_offer_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "mission", nullable = false, columnDefinition = "text")
    List<String> missions = new ArrayList<>();
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    List<JobRequirement> requirements = new ArrayList<>();

    protected JobOffer() {}

    JobOffer(UUID id, String text, String url, Instant at, JobOfferExtraction extraction, String snapshot) {
        this.id = id;
        originalText = text;
        sourceUrl = url;
        analyzedAt = at;
        initialExtraction = snapshot;
        company = extraction.company();
        position = extraction.position();
        sourceLanguage = extraction.sourceLanguage();
        summary = extraction.summary();
        city = extraction.location().city();
        region = extraction.location().region();
        country = extraction.location().country();
        contractType = extraction.contractType();
        workArrangementType = extraction.workArrangement().type();
        remoteArea = extraction.workArrangement().remoteArea();
        onSiteDaysPerWeek = extraction.workArrangement().onSiteDaysPerWeek();
        missions.addAll(extraction.missions());
        for (int i = 0; i < extraction.requirements().size(); i++) {
            requirements.add(new JobRequirement(this, i, extraction.requirements().get(i)));
        }
    }

    JobOfferDtos.Response toResponse() {
        var current = new JobOfferReviewDtos.CurrentExtraction(company, position, new Location(city, region, country),
                new WorkArrangement(workArrangementType, remoteArea, onSiteDaysPerWeek), contractType,
                sourceLanguage, summary, List.copyOf(missions),
                requirements.stream().map(JobRequirement::toData).toList());
        return new JobOfferDtos.Response(id, originalText, sourceUrl, analyzedAt, reviewStatus, reviewBypassedAt, current);
    }
}
