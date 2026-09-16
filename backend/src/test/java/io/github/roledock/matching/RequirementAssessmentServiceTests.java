package io.github.roledock.matching;

import io.github.roledock.joboffer.JobOfferReviewDtos.*;
import io.github.roledock.joboffer.ReviewStatus;
import io.github.roledock.profile.ProfileDtos.*;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import static io.github.roledock.matching.RequirementAssessment.*;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RequirementAssessmentServiceTests {
    final RequirementAssessmentService engine = new RequirementAssessmentService();
    final LocalDate today = LocalDate.of(2026, 1, 1);
    final UUID skillId = UUID.randomUUID();
    Response profile(List<SkillData> skills, List<ExperienceData> experiences,
                     List<LanguageData> languages, List<CertificationData> certificates, boolean complete) {
        return new Response(UUID.randomUUID(), null, List.of(), null, null, null, List.of(), List.of(),
                List.of(), experiences, skills, List.of(), languages, certificates, List.of(), null, complete);
    }
    Response skill(String label) { return profile(List.of(new SkillData(skillId, label, null)), List.of(), List.of(), List.of(), false); }
    CurrentRequirement requirement(String label, RequirementCategory category, Constraint constraint, boolean blocker, RequirementKind kind) {
        return new CurrentRequirement(UUID.randomUUID(), RequirementSource.USER_ADDED, null, label, category,
                kind, Centrality.CORE, Explicitness.EXPLICIT, blocker, blocker ? label : null, constraint, null);
    }
    RequirementAssessment assess(CurrentRequirement r, Response p) { return engine.assess(r, p, ReviewStatus.CONFIRMED, today); }
    @ParameterizedTest @CsvSource({"Java,java", "JavaScript,JS", "TypeScript,TS", "PostgreSQL,Postgres", "Spring Boot,SpringBoot"})
    void exactAndTrueAliases(String requested, String actual) {
        var a = assess(requirement(requested, RequirementCategory.TECH_SKILL, null, false, RequirementKind.REQUIRED), skill(actual));
        assertThat(a.status()).isEqualTo(Status.MATCH);
        assertThat(a.transferRelation()).isEqualTo(TransferRelation.EXACT);
        assertThat(a.evidence()).extracting(EvidenceReference::id).containsExactly(skillId);
    }
    @ParameterizedTest @CsvSource({"Angular,React", "Java,JavaScript", "Docker,Kubernetes", "C,C++", ".NET,ASP.NET", "SQL,NoSQL"})
    void neighboringLabelsAreNotMatches(String requested, String actual) {
        var a = assess(requirement(requested, RequirementCategory.TECH_SKILL, null, false, RequirementKind.REQUIRED), skill(actual));
        assertThat(a.status()).isEqualTo(Status.UNKNOWN);
        assertThat(a.transferRelation()).isEqualTo(TransferRelation.NONE);
    }
    @Test void unknownAndMissingDependOnExplicitCompleteness() {
        var r = requirement("Credential Alpha", RequirementCategory.CERTIFICATION, null, true, RequirementKind.REQUIRED);
        assertThat(assess(r, profile(List.of(), List.of(), List.of(), List.of(), false)).status()).isEqualTo(Status.UNKNOWN);
        var a = assess(r, profile(List.of(), List.of(), List.of(), List.of(), true));
        assertThat(a.status()).isEqualTo(Status.MISSING);
        assertThat(a.eligibilityEffect()).isEqualTo(EligibilityEffect.BLOCK);
        assertThat(engine.assess(r, profile(List.of(), List.of(), List.of(), List.of(), true), ReviewStatus.UNREVIEWED, today).eligibilityEffect())
                .isEqualTo(EligibilityEffect.POSSIBLE_BLOCK);
    }
    @Test void exactCertificateHasRealEvidence() {
        var c = new CertificationData(UUID.randomUUID(), "Credential Alpha", "Fictional issuer", today.minusYears(1), today.plusYears(1), null, null);
        var a = assess(requirement(c.name(), RequirementCategory.CERTIFICATION, null, true, RequirementKind.REQUIRED),
                profile(List.of(), List.of(), List.of(), List.of(c), false));
        assertThat(a.status()).isEqualTo(Status.MATCH);
        assertThat(a.evidence()).extracting(EvidenceReference::id).containsExactly(c.id());
        assertThat(a.eligibilityEffect()).isEqualTo(EligibilityEffect.NONE);
    }
    @Test void optionalAbsentCertificationIsNeverABlocker() {
        var a = assess(requirement("Credential Alpha", RequirementCategory.CERTIFICATION, null, true, RequirementKind.PREFERRED),
                profile(List.of(), List.of(), List.of(), List.of(), true));
        assertThat(a.eligibilityEffect()).isEqualTo(EligibilityEffect.NONE);
    }
    @Test void ambiguousBlockerAndMissingProfileRemainUnknown() {
        var a = assess(requirement("Autorisation de travail", RequirementCategory.WORK_AUTHORIZATION, null, true, RequirementKind.REQUIRED), null);
        assertThat(a.status()).isEqualTo(Status.UNKNOWN);
        assertThat(a.eligibilityEffect()).isEqualTo(EligibilityEffect.POSSIBLE_BLOCK);
    }
    @Test void softSkillSelfDeclarationIsInsufficient() {
        assertThat(assess(requirement("Communication", RequirementCategory.SOFT_SKILL, null, false, RequirementKind.REQUIRED), skill("Communication")).status()).isEqualTo(Status.UNKNOWN);
    }
    @ParameterizedTest @CsvSource({"3,MATCH", "2,PARTIAL", "1,UNKNOWN"})
    void relevantDurationUsesLinkedExperience(int years, Status expected) {
        var e = new ExperienceData(UUID.randomUUID(), "Fictional company", "Developer", null, today.minusYears(years), today, false, null, List.of(), List.of(skillId));
        var p = profile(List.of(new SkillData(skillId, "Java", null)), List.of(e), List.of(), List.of(), false);
        var r = requirement("Java", RequirementCategory.EXPERIENCE, new Constraint(ConstraintOperator.AT_LEAST, "3", "years"), true, RequirementKind.REQUIRED);
        var a = assess(r, p);
        assertThat(a.status()).isEqualTo(expected);
        assertThat(a.eligibilityEffect()).isNotEqualTo(EligibilityEffect.BLOCK);
        assertThat(a.evidence()).extracting(EvidenceReference::id).contains(skillId, e.id()).doesNotHaveDuplicates();
        assertThat(assess(r, p)).isEqualTo(a);
    }
    @Test void durationWithoutDatesIsUnknown() {
        assertThat(assess(requirement("Java", RequirementCategory.EXPERIENCE,
                new Constraint(ConstraintOperator.AT_LEAST, "3", "years"), false, RequirementKind.REQUIRED), skill("Java")).status()).isEqualTo(Status.UNKNOWN);
    }
    @Test void overlapsDoNotDoubleCountAndRepeatedRequirementsDoNotDuplicateEvidence() {
        var e = new ExperienceData(UUID.randomUUID(), "Fictional", "Developer", null, today.minusYears(2), today, false, null, List.of(), List.of(skillId, skillId));
        var p = profile(List.of(new SkillData(skillId, "Java", null)), List.of(e, e), List.of(), List.of(), false);
        var r = requirement("Java", RequirementCategory.EXPERIENCE, new Constraint(ConstraintOperator.AT_LEAST, "3", "years"), false, RequirementKind.REQUIRED);
        assertThat(assess(r, p).status()).isEqualTo(Status.PARTIAL);
        assertThat(assess(r, p).evidence()).hasSize(2);
        assertThat(assess(r, p)).isEqualTo(assess(r, p));
    }
    @ParameterizedTest @CsvSource({"C1,MATCH", "B1,MISSING", "fluent,UNKNOWN"})
    void languageUsesOnlyExplicitCefr(String level, Status expected) {
        var p = profile(List.of(), List.of(), List.of(new LanguageData(UUID.randomUUID(), "English", level)), List.of(), false);
        assertThat(assess(requirement("English", RequirementCategory.LANGUAGE,
                new Constraint(ConstraintOperator.AT_LEAST, "B2", "CEFR"), false, RequirementKind.REQUIRED), p).status()).isEqualTo(expected);
    }

    @Test void conditionalBlockerIsNeverDefinitiveEvenWithMissingCertification() {
        var r = requirement("Credential Alpha", RequirementCategory.CERTIFICATION, null, true, RequirementKind.REQUIRED);
        r = new CurrentRequirement(r.id(), r.source(), r.rawText(), r.canonicalLabel(), r.category(), r.requirementKind(), r.centrality(),
                r.explicitness(), true, "Required only for regulated assignments", null, null);
        assertThat(assess(r, profile(List.of(), List.of(), List.of(), List.of(), true)).eligibilityEffect()).isEqualTo(EligibilityEffect.POSSIBLE_BLOCK);
    }
    @Test void unknownCertificationAndExpiredCertificateAreNotConfidentMatches() {
        var p = profile(List.of(), List.of(), List.of(), List.of(), true);
        assertThat(assess(requirement("Alpha ou Beta", RequirementCategory.CERTIFICATION, null, false, RequirementKind.REQUIRED), p).status()).isEqualTo(Status.UNKNOWN);
        var expired = new CertificationData(UUID.randomUUID(), "Credential Alpha", null, today.minusYears(2), today.minusDays(1), null, null);
        assertThat(assess(requirement(expired.name(), RequirementCategory.CERTIFICATION, null, false, RequirementKind.REQUIRED),
                profile(List.of(), List.of(), List.of(), List.of(expired), true)).status()).isEqualTo(Status.UNKNOWN);
    }
    @Test void contextualRequirementIsNotApplicableAndSkillConstraintIsNotIgnored() {
        assertThat(assess(requirement("Java", RequirementCategory.TECH_SKILL, null, false, RequirementKind.CONTEXTUAL), null).status()).isEqualTo(Status.NOT_APPLICABLE);
        assertThat(assess(requirement("Java", RequirementCategory.TECH_SKILL, new Constraint(ConstraintOperator.AT_LEAST, "3", "years"), false, RequirementKind.REQUIRED), skill("Java")).status()).isEqualTo(Status.UNKNOWN);
    }
    @Test void educationRequiresTheSameDegreeWithoutInventingEquivalence() {
        var e = new EducationData(UUID.randomUUID(), "Fictional university", "Master informatique", null, null, today.minusYears(1), null);
        var p = new Response(UUID.randomUUID(), null, List.of(), null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(e), List.of(), List.of(), List.of(), null, false);
        var matched = assess(requirement("Master informatique", RequirementCategory.EDUCATION, null, false, RequirementKind.REQUIRED), p);
        assertThat(matched.status()).isEqualTo(Status.MATCH);
        assertThat(matched.evidence()).extracting(EvidenceReference::id).containsExactly(e.id());
        assertThat(assess(requirement("Diplôme ingénieur", RequirementCategory.EDUCATION, null, false, RequirementKind.REQUIRED), p).status()).isEqualTo(Status.UNKNOWN);
    }

    @Test void completenessEvidenceReferencesTheActualProfile() {
        var p = profile(List.of(), List.of(), List.of(), List.of(), true);
        var a = assess(requirement("Credential Alpha", RequirementCategory.CERTIFICATION, null, false, RequirementKind.REQUIRED), p);
        assertThat(a.evidence()).extracting(EvidenceReference::id).containsExactly(p.id());
    }
    @Test void nonBreakingWhitespaceDoesNotPreventAnExactSkillMatch() {
        assertThat(assess(requirement("\u00a0Spring\u00a0Boot\u00a0", RequirementCategory.TECH_SKILL, null, false, RequirementKind.REQUIRED), skill("Spring Boot")).status()).isEqualTo(Status.MATCH);
    }
}
