package io.github.roledock.joboffer;

import io.github.roledock.matching.RequirementAssessment;
import io.github.roledock.matching.RequirementAssessmentService;
import io.github.roledock.matching.OfferAnalysis;
import io.github.roledock.matching.OfferAnalysisService;
import io.github.roledock.profile.ProfileService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class JobOfferAssessmentService {
    private final JobOfferRepository offers;
    private final ProfileService profiles;
    private final RequirementAssessmentService assessments;
    private final OfferAnalysisService analysis;
    JobOfferAssessmentService(JobOfferRepository offers, ProfileService profiles, RequirementAssessmentService assessments,
                              OfferAnalysisService analysis) {
        this.offers = offers; this.profiles = profiles; this.assessments = assessments;
        this.analysis = analysis;
    }
    record Response(LocalDate assessedOn, List<RequirementAssessment> assessments) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    Optional<OfferAnalysis> analyze(UUID id) {
        return offers.findById(id).map(entity -> {
            var offer = entity.toResponse();
            var profile = profiles.getCurrentProfile().orElse(null);
            var date = LocalDate.now(ZoneOffset.UTC);
            var results = offer.extraction().requirements().stream()
                    .map(r -> assessments.assess(r, profile, offer.reviewStatus(), date)).toList();
            return analysis.analyze(offer, profile, results, date);
        });
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    Optional<Response> get(UUID id) {
        return offers.findById(id).map(entity -> {
            var offer = entity.toResponse();
            var profile = profiles.getCurrentProfile().orElse(null);
            var date = LocalDate.now(ZoneOffset.UTC);
            return new Response(date, offer.extraction().requirements().stream()
                    .map(r -> assessments.assess(r, profile, offer.reviewStatus(), date)).toList());
        });
    }
}
