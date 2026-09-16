package io.github.roledock.matching;

import io.github.roledock.joboffer.JobOfferDtos;
import io.github.roledock.joboffer.JobOfferReviewDtos.CurrentRequirement;
import io.github.roledock.joboffer.ReviewStatus;
import io.github.roledock.profile.ProfileDtos;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import static io.github.roledock.matching.RequirementAssessment.*;
import static io.github.roledock.matching.OfferAnalysis.*;
import static io.github.roledock.matching.ScoringRules.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** Pure aggregation over current projections and assessments; no matching or provider calls. */
@Service
public class OfferAnalysisService {
    private static final Set<RequirementCategory> ELIGIBILITY_CATEGORIES = Set.of(
            RequirementCategory.WORK_AUTHORIZATION, RequirementCategory.LOCATION,
            RequirementCategory.AVAILABILITY, RequirementCategory.CONTRACT);

    public OfferAnalysis analyze(JobOfferDtos.Response offer, ProfileDtos.Response profile,
                                 List<RequirementAssessment> assessments, LocalDate assessedOn) {
        var byId = new HashMap<UUID, RequirementAssessment>();
        for (var a : assessments) if (byId.put(a.requirementId(), a) != null)
            throw new IllegalArgumentException("Duplicate assessment identifier");
        var requirements = offer.extraction().requirements().stream().sorted(Comparator.comparing(CurrentRequirement::id)).toList();
        var ids = new HashSet<UUID>();
        requirements.forEach(r -> { if (!ids.add(r.id())) throw new IllegalArgumentException("Duplicate requirement identifier"); });
        if (!ids.equals(byId.keySet())) throw new IllegalArgumentException("Assessments must cover exactly the current requirements");

        var unique = new LinkedHashMap<CurrentRequirement, CurrentRequirement>();
        var contributions = new ArrayList<Contribution>();
        var gaps = new ArrayList<CriticalGap>();
        double numerator = 0, denominator = 0;
        int unknown = 0, requiredUnknown = 0, lowConfidence = 0, classificationUnknown = 0, unsupported = 0, requiredGaps = 0;
        boolean importantUnknown = false, confirmedBlock = false, possibleBlock = false, eligibilityUnknown = false;
        for (var r : requirements) {
            var a = byId.get(r.id());
            var previous = unique.putIfAbsent(duplicateKey(r), r);
            if (previous != null) {
                if (!assessmentKey(a).equals(assessmentKey(byId.get(previous.id()))))
                    throw new IllegalArgumentException("Exact duplicate requirements have inconsistent assessments");
                contributions.add(new Contribution(r.id(), weight(r), null, false, previous.id(),
                        "Doublon exact : pris en compte une seule fois."));
                continue;
            }
            var coverage = coverage(r, a);
            double weight = weight(r);
            boolean included = weight > 0 && coverage.value() != null;
            String rationale = coverage.rationale();
            if (r.requirementKind() == RequirementKind.CONTEXTUAL) rationale = "Contexte : poids nul.";
            else if (r.requirementKind() == RequirementKind.UNKNOWN || r.centrality() == Centrality.UNKNOWN)
                rationale = "Importance ou centralité inconnue : exclue du calcul.";
            contributions.add(new Contribution(r.id(), weight, coverage.value(), included, null, rationale));
            if (included) { numerator += weight * coverage.value(); denominator += weight; }
            boolean applicable = r.requirementKind() != RequirementKind.CONTEXTUAL && a.status() != Status.NOT_APPLICABLE;
            if (!applicable) continue;
            boolean required = r.requirementKind() == RequirementKind.REQUIRED;
            boolean core = r.centrality() == Centrality.CORE;
            boolean unresolved = a.status() == Status.UNKNOWN || (a.status() == Status.PARTIAL && coverage.value() == null);
            if (a.status() == Status.UNKNOWN) { unknown++; if (required) requiredUnknown++; }
            if (a.status() == Status.PARTIAL && coverage.value() == null) unsupported++;
            if (a.assessmentConfidence() == AssessmentConfidence.LOW) lowConfidence++;
            if (r.requirementKind() == RequirementKind.UNKNOWN || r.centrality() == Centrality.UNKNOWN) classificationUnknown++;
            importantUnknown |= required && (core || r.centrality() == Centrality.UNKNOWN) && unresolved;
            if (required && (a.status() == Status.MISSING || a.status() == Status.PARTIAL)) requiredGaps++;
            if (required && core && (a.status() == Status.MISSING
                    || (a.status() == Status.PARTIAL && coverage.value() != null && coverage.value() <= SEVERE_PARTIAL_MAX)))
                gaps.add(new CriticalGap(r.id(), r.canonicalLabel(), a.status(), a.rationale()));
            boolean safeBlock = a.eligibilityEffect() == EligibilityEffect.BLOCK
                    && offer.reviewStatus() != ReviewStatus.UNREVIEWED && required
                    && Boolean.TRUE.equals(r.hardBlockerCandidate()) && r.explicitness() == Explicitness.EXPLICIT
                    && MatchingLabels.same(r.blockerCondition(), r.canonicalLabel())
                    && a.status() == Status.MISSING && a.assessmentConfidence() == AssessmentConfidence.HIGH;
            confirmedBlock |= safeBlock;
            possibleBlock |= a.eligibilityEffect() == EligibilityEffect.POSSIBLE_BLOCK
                    || (a.eligibilityEffect() == EligibilityEffect.BLOCK && !safeBlock);
            eligibilityUnknown |= required && unresolved && (Boolean.TRUE.equals(r.hardBlockerCandidate())
                    || ELIGIBILITY_CATEGORIES.contains(r.category()));
        }
        var reasons = new ArrayList<String>();
        if (unknown > 0) reasons.add(unknown + " exigence(s) inconnue(s), dont " + requiredUnknown + " requise(s), exclue(s) du calcul.");
        if (lowConfidence > 0) reasons.add(lowConfidence + " évaluation(s) reposent sur des informations peu fiables.");
        if (classificationUnknown > 0) reasons.add(classificationUnknown + " exigence(s) sans importance ou centralité déterminée.");
        if (unsupported > 0) reasons.add(unsupported + " couverture(s) partielle(s) sans règle numérique applicable.");
        if (possibleBlock) reasons.add("Une condition potentiellement bloquante reste à vérifier.");
        if (offer.reviewStatus() == ReviewStatus.UNREVIEWED) reasons.add("L’extraction de l’offre n’a pas été vérifiée.");
        if (profile == null) reasons.add("Aucun profil candidat enregistré.");
        if (denominator == 0) reasons.add("Aucune exigence pondérée évaluable : couverture indisponible.");
        var level = profile == null || denominator == 0 || importantUnknown || possibleBlock || eligibilityUnknown
                || unknown >= MANY_UNCERTAINTIES || lowConfidence >= MANY_UNCERTAINTIES
                ? UncertaintyLevel.HIGH : reasons.isEmpty() ? UncertaintyLevel.LOW : UncertaintyLevel.MEDIUM;
        var eligibility = confirmedBlock ? Eligibility.NOT_ELIGIBLE
                : profile == null || requirements.isEmpty() || eligibilityUnknown ? Eligibility.UNKNOWN
                : possibleBlock ? Eligibility.ELIGIBLE_WITH_CONSTRAINT : Eligibility.ELIGIBLE;
        // Round only the public score; recommendations use the unrounded ratio.
        Double rawScore = denominator == 0 ? null : 100 * numerator / denominator;
        Double score = rawScore == null ? null : BigDecimal.valueOf(rawScore).setScale(2, RoundingMode.HALF_UP).doubleValue();
        var recommendation = recommend(eligibility, rawScore, level, gaps.size(), requiredGaps);
        return new OfferAnalysis(assessedOn, offer.reviewStatus(), score, eligibility, recommendation, gaps,
                new Uncertainty(level, reasons), requirements.stream().map(r -> byId.get(r.id())).toList(), contributions);
    }

    private Recommendation recommend(Eligibility eligibility, Double score, UncertaintyLevel uncertainty, int gaps, int requiredGaps) {
        if (eligibility == Eligibility.NOT_ELIGIBLE) return Recommendation.SKIP_CONFIRMED_BLOCKER;
        if (eligibility == Eligibility.UNKNOWN || eligibility == Eligibility.ELIGIBLE_WITH_CONSTRAINT
                || score == null || uncertainty == UncertaintyLevel.HIGH) return Recommendation.VERIFY_FIRST;
        if (score >= STRONG_COVERAGE && gaps == 0 && requiredGaps == 0) return Recommendation.APPLY_NOW;
        if (score >= BRIDGE_COVERAGE && gaps < SEVERAL_CRITICAL_GAPS) return Recommendation.APPLY_WITH_BRIDGE;
        return Recommendation.STRETCH;
    }

    // No semantic similarity: keep constraints, classification and provenance identical.
    private CurrentRequirement duplicateKey(CurrentRequirement r) {
        return new CurrentRequirement(null, r.source(), null, MatchingLabels.text(r.canonicalLabel()), r.category(),
                r.requirementKind(), r.centrality(), r.explicitness(), r.hardBlockerCandidate(),
                MatchingLabels.text(r.blockerCondition()), r.constraint(), r.extractionConfidence());
    }
    private RequirementAssessment assessmentKey(RequirementAssessment a) {
        // Explanatory prose may contain the differently cased source label.
        return new RequirementAssessment(null, a.status(), a.transferRelation(), a.evidenceStrength(),
                a.assessmentConfidence(), a.eligibilityEffect(), a.evidence(), "", null);
    }
}
