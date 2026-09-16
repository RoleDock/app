package io.github.roledock.matching;

import io.github.roledock.joboffer.JobOfferDtos;
import io.github.roledock.joboffer.JobOfferReviewDtos.*;
import io.github.roledock.joboffer.ReviewStatus;
import io.github.roledock.profile.ProfileDtos;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import static io.github.roledock.matching.RequirementAssessment.*;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OfferAnalysisServiceTests {
    final OfferAnalysisService service = new OfferAnalysisService();
    final ProfileDtos.Response profile = new ProfileDtos.Response(UUID.randomUUID(), null, List.of(), null, null,
            null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
    final LocalDate date = LocalDate.of(2026, 1, 1);
    record Row(CurrentRequirement requirement, RequirementAssessment assessment) {}
    Row row(String label, RequirementKind kind, Centrality centrality, Status status, TransferRelation relation,
            EligibilityEffect effect, RequirementCategory category) {
        var id = UUID.randomUUID();
        return new Row(new CurrentRequirement(id, RequirementSource.USER_ADDED, null, label, category, kind,
                centrality, Explicitness.EXPLICIT, effect != EligibilityEffect.NONE,
                effect != EligibilityEffect.NONE ? label : null, null, null),
                new RequirementAssessment(id, status, relation, EvidenceStrength.STRONG, AssessmentConfidence.HIGH,
                        effect, List.of(), "Explication fictive : " + label, null));
    }
    Row row(String label, Status status) {
        return row(label, RequirementKind.REQUIRED, Centrality.CORE, status, TransferRelation.EXACT,
                EligibilityEffect.NONE, RequirementCategory.TECH_SKILL);
    }
    JobOfferDtos.Response offer(ReviewStatus review, List<Row> rows) {
        return new JobOfferDtos.Response(UUID.randomUUID(), "Fictional", null, Instant.EPOCH, review, null,
                new CurrentExtraction(null, null, new Location(null, null, null),
                        new WorkArrangement(WorkArrangementType.UNKNOWN, null, null), ContractType.UNKNOWN,
                        null, null, List.of(), rows.stream().map(Row::requirement).toList()));
    }
    OfferAnalysis analyze(ReviewStatus review, Row... rows) {
        return service.analyze(offer(review, List.of(rows)), profile, Arrays.stream(rows).map(Row::assessment).toList(), date);
    }
    OfferAnalysis analyze(Row... rows) { return analyze(ReviewStatus.CONFIRMED, rows); }

    @Test void perfectCoverageAndStrongRecommendation() {
        var result = analyze(row("Java", Status.MATCH));
        assertThat(result.coverageScore()).isEqualTo(100);
        assertThat(result.eligibility()).isEqualTo(OfferAnalysis.Eligibility.ELIGIBLE);
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.APPLY_NOW);
    }
    @ParameterizedTest @CsvSource({"REQUIRED,CORE,50", "PREFERRED,CORE,80", "REQUIRED,SUPPORTING,66.67",
            "REQUIRED,INCIDENTAL,80", "PREFERRED,INCIDENTAL,94.12", "CONTEXTUAL,CORE,100",
            "UNKNOWN,CORE,100", "REQUIRED,UNKNOWN,100"})
    void centralizedWeights(RequirementKind kind, Centrality centrality, double expected) {
        var result = analyze(row("Java", Status.MATCH), row("Other", kind, centrality, Status.MISSING,
                TransferRelation.NONE, EligibilityEffect.NONE, RequirementCategory.CERTIFICATION));
        assertThat(result.coverageScore()).isEqualTo(expected);
    }
    @ParameterizedTest @CsvSource({"UNKNOWN,100", "NOT_APPLICABLE,100", "MISSING,50"})
    void exclusionsAndZero(Status status, double expected) {
        assertThat(analyze(row("Java", Status.MATCH), row("Other", status)).coverageScore()).isEqualTo(expected);
    }
    @ParameterizedTest @CsvSource({"EQUIVALENT,75", "ADJACENT,50", "PREREQUISITE,25"})
    void partialTransfer(TransferRelation relation, double expected) {
        assertThat(analyze(row("Other", RequirementKind.REQUIRED, Centrality.CORE, Status.PARTIAL,
                relation, EligibilityEffect.NONE, RequirementCategory.TECH_SKILL)).coverageScore()).isEqualTo(expected);
    }
    @Test void unsupportedPartialIsExcludedWithExplanation() {
        var result = analyze(row("Other", RequirementKind.REQUIRED, Centrality.CORE, Status.PARTIAL,
                TransferRelation.NONE, EligibilityEffect.NONE, RequirementCategory.OTHER));
        assertThat(result.coverageScore()).isNull();
        assertThat(result.uncertainty().reasons()).isNotEmpty();
    }
    @Test void realExperiencePartialExactDoesNotMeanFullCoverage() {
        var skillId = UUID.randomUUID();
        var p = new ProfileDtos.Response(UUID.randomUUID(), null, List.of(), null, null, null, List.of(), List.of(), List.of(),
                List.of(new ProfileDtos.ExperienceData(UUID.randomUUID(), "Fictional", "Developer", null,
                        date.minusYears(2), date, false, null, List.of(), List.of(skillId))),
                List.of(new ProfileDtos.SkillData(skillId, "Java", null)), List.of(), List.of(), List.of(), List.of(), null, false);
        var r = new CurrentRequirement(UUID.randomUUID(), RequirementSource.USER_ADDED, null, "Java",
                RequirementCategory.EXPERIENCE, RequirementKind.REQUIRED, Centrality.CORE, Explicitness.EXPLICIT,
                false, null, new Constraint(ConstraintOperator.AT_LEAST, "3", "years"), null);
        var assessment = new RequirementAssessmentService().assess(r, p, ReviewStatus.CONFIRMED, date);
        assertThat(assessment.status()).isEqualTo(Status.PARTIAL);
        var result = service.analyze(offer(ReviewStatus.CONFIRMED, List.of(new Row(r, assessment))), p, List.of(assessment), date);
        assertThat(result.coverageScore()).isEqualTo(75);
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.APPLY_WITH_BRIDGE);
    }
    @Test void highScoreCannotHideCriticalGap() {
        var rows = new ArrayList<Row>();
        for (int i = 0; i < 10; i++) rows.add(row("Skill " + i, Status.MATCH));
        var missing = row("Credential", Status.MISSING); rows.add(missing);
        var result = analyze(rows.toArray(Row[]::new));
        assertThat(result.coverageScore()).isGreaterThan(90);
        assertThat(result.criticalGaps()).extracting(OfferAnalysis.CriticalGap::requirementId).containsExactly(missing.requirement.id());
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.APPLY_WITH_BRIDGE);
    }
    @Test void noDenominatorIsUnavailableNotZero() {
        assertThat(analyze().coverageScore()).isNull();
        assertThat(analyze(row("Java", Status.UNKNOWN)).coverageScore()).isNull();
        assertThat(analyze().recommendation()).isEqualTo(OfferAnalysis.Recommendation.VERIFY_FIRST);
    }
    @ParameterizedTest @CsvSource({"CONFIRMED,BLOCK,NOT_ELIGIBLE,SKIP_CONFIRMED_BLOCKER",
            "CORRECTED,BLOCK,NOT_ELIGIBLE,SKIP_CONFIRMED_BLOCKER",
            "UNREVIEWED,BLOCK,ELIGIBLE_WITH_CONSTRAINT,VERIFY_FIRST",
            "CONFIRMED,POSSIBLE_BLOCK,ELIGIBLE_WITH_CONSTRAINT,VERIFY_FIRST",
            "UNREVIEWED,POSSIBLE_BLOCK,ELIGIBLE_WITH_CONSTRAINT,VERIFY_FIRST"})
    void blockerSafety(ReviewStatus review, EligibilityEffect effect, OfferAnalysis.Eligibility eligibility,
                       OfferAnalysis.Recommendation recommendation) {
        var result = analyze(review, row("Credential", RequirementKind.REQUIRED, Centrality.CORE, Status.MISSING,
                TransferRelation.NONE, effect, RequirementCategory.CERTIFICATION));
        assertThat(result.eligibility()).isEqualTo(eligibility);
        assertThat(result.recommendation()).isEqualTo(recommendation);
    }
    @Test void highScoreDoesNotOverrideBlockAndLowScoreDoesNotCreateOne() {
        var rows = new ArrayList<Row>();
        for (int i = 0; i < 10; i++) rows.add(row("Skill " + i, Status.MATCH));
        rows.add(row("Credential", RequirementKind.REQUIRED, Centrality.CORE, Status.MISSING,
                TransferRelation.NONE, EligibilityEffect.BLOCK, RequirementCategory.CERTIFICATION));
        assertThat(analyze(rows.toArray(Row[]::new)).recommendation()).isEqualTo(OfferAnalysis.Recommendation.SKIP_CONFIRMED_BLOCKER);
        var low = analyze(row("Credential", Status.MISSING));
        assertThat(low.eligibility()).isEqualTo(OfferAnalysis.Eligibility.ELIGIBLE);
        assertThat(low.recommendation()).isEqualTo(OfferAnalysis.Recommendation.STRETCH);
    }
    @Test void importantUnknownRequiresVerificationButDoesNotProveIneligibility() {
        var result = analyze(row("Java", Status.MATCH), row("Angular", Status.UNKNOWN));
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.VERIFY_FIRST);
        assertThat(result.uncertainty().level()).isEqualTo(OfferAnalysis.UncertaintyLevel.HIGH);
        var eligibilityUnknown = analyze(row("Work permit", RequirementKind.REQUIRED, Centrality.CORE,
                Status.UNKNOWN, TransferRelation.NONE, EligibilityEffect.NONE, RequirementCategory.WORK_AUTHORIZATION));
        assertThat(eligibilityUnknown.eligibility()).isEqualTo(OfferAnalysis.Eligibility.UNKNOWN);
    }
    @Test void resolvingUnknownChangesDenominatorOnlyAfterResolution() {
        assertThat(analyze(row("Java", Status.MATCH), row("Credential", Status.UNKNOWN)).coverageScore()).isEqualTo(100);
        assertThat(analyze(row("Java", Status.MATCH), row("Credential", Status.MISSING)).coverageScore()).isEqualTo(50);
    }
    @Test void duplicatesDoNotInflateAndAllEvidenceRemainsVisible() {
        var java = row("Java", Status.MATCH); var missing = row("Credential", Status.MISSING);
        var result = analyze(java, row(" java ", Status.MATCH), missing);
        assertThat(result.coverageScore()).isEqualTo(50);
        assertThat(result.requirementAssessments()).hasSize(3);
        assertThat(result.contributions().stream().filter(c -> c.duplicateOf() != null)).hasSize(1);
    }
    @Test void severePartialIsCriticalButAdjacentRemainsBridgeable() {
        var severe = row("Skill", RequirementKind.REQUIRED, Centrality.CORE, Status.PARTIAL,
                TransferRelation.PREREQUISITE, EligibilityEffect.NONE, RequirementCategory.TECH_SKILL);
        assertThat(analyze(severe).criticalGaps()).hasSize(1);
        var adjacent = row("Skill", RequirementKind.REQUIRED, Centrality.CORE, Status.PARTIAL,
                TransferRelation.ADJACENT, EligibilityEffect.NONE, RequirementCategory.TECH_SKILL);
        var result = analyze(row("Java", Status.MATCH), adjacent);
        assertThat(result.criticalGaps()).isEmpty();
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.APPLY_WITH_BRIDGE);
    }
    @Test void severalCriticalGapsRemainStretchEvenAtHighCoverage() {
        var rows = new ArrayList<Row>();
        for (int i = 0; i < 10; i++) rows.add(row("Skill " + i, Status.MATCH));
        rows.add(row("Credential A", Status.MISSING)); rows.add(row("Credential B", Status.MISSING));
        assertThat(analyze(rows.toArray(Row[]::new)).recommendation()).isEqualTo(OfferAnalysis.Recommendation.STRETCH);
    }
    @Test void lowConfidenceAndUnreviewedExtractionAreExplained() {
        var high = row("Java", Status.MATCH);
        var low = new Row(high.requirement(), new RequirementAssessment(high.requirement().id(), Status.MATCH,
                TransferRelation.EXACT, EvidenceStrength.WEAK, AssessmentConfidence.LOW, EligibilityEffect.NONE,
                List.of(), "Fictional", null));
        var result = analyze(ReviewStatus.UNREVIEWED, low);
        assertThat(result.uncertainty().level()).isEqualTo(OfferAnalysis.UncertaintyLevel.MEDIUM);
        assertThat(result.uncertainty().reasons()).hasSize(2);
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.APPLY_NOW);
    }
    @Test void severalUnknownsRequireVerification() {
        var result = analyze(row("Java", Status.MATCH), row("A", Status.UNKNOWN), row("B", Status.UNKNOWN), row("C", Status.UNKNOWN));
        assertThat(result.uncertainty().reasons().getFirst()).contains("3 exigence(s)");
        assertThat(result.coverageScore()).isEqualTo(100);
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.VERIFY_FIRST);
    }
    @Test void missingProfileCannotConcludeEligibility() {
        var rows = List.of(row("Java", Status.UNKNOWN));
        var result = service.analyze(offer(ReviewStatus.CONFIRMED, rows), null, rows.stream().map(Row::assessment).toList(), date);
        assertThat(result.eligibility()).isEqualTo(OfferAnalysis.Eligibility.UNKNOWN);
    }
    @Test void refusesIncompleteOrStaleAssessmentSets() {
        var rows = List.of(row("Java", Status.MATCH));
        assertThatThrownBy(() -> service.analyze(offer(ReviewStatus.CONFIRMED, rows), profile, List.of(), date))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void differentClassificationIsNotAnExactDuplicate() {
        var optional = row("Java", RequirementKind.PREFERRED, Centrality.CORE, Status.MATCH,
                TransferRelation.EXACT, EligibilityEffect.NONE, RequirementCategory.TECH_SKILL);
        var result = analyze(row("Java", Status.MATCH), optional, row("Credential", Status.MISSING));
        assertThat(result.coverageScore()).isEqualTo(55.56);
        assertThat(result.contributions()).allMatch(c -> c.duplicateOf() == null);
    }
    @Test void forgedBlockWithoutExplicitBlockerIsOnlyAConstraint() {
        var r = row("Java", Status.MISSING);
        var a = new RequirementAssessment(r.requirement().id(), Status.MISSING, TransferRelation.NONE,
                EvidenceStrength.STRONG, AssessmentConfidence.HIGH, EligibilityEffect.BLOCK, List.of(), "Fictional", null);
        var result = analyze(new Row(r.requirement(), a));
        assertThat(result.eligibility()).isEqualTo(OfferAnalysis.Eligibility.ELIGIBLE_WITH_CONSTRAINT);
        assertThat(result.recommendation()).isEqualTo(OfferAnalysis.Recommendation.VERIFY_FIRST);
    }
    @Test void deterministicAndOrderIndependent() {
        var rows = List.of(row("Java", Status.MATCH), row("Credential", Status.MISSING));
        var o = offer(ReviewStatus.CONFIRMED, rows);
        var a = rows.stream().map(Row::assessment).toList();
        assertThat(service.analyze(o, profile, a, date)).isEqualTo(service.analyze(o, profile, a, date));
        assertThat(analyze(rows.get(1), rows.get(0)).coverageScore()).isEqualTo(analyze(rows.get(0), rows.get(1)).coverageScore());
    }
}
