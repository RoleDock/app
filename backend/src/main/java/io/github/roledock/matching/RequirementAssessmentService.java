package io.github.roledock.matching;

import io.github.roledock.joboffer.JobOfferReviewDtos.CurrentRequirement;
import io.github.roledock.joboffer.ReviewStatus;
import io.github.roledock.profile.ProfileDtos.*;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import static io.github.roledock.matching.RequirementAssessment.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RequirementAssessmentService {
    private record Finding(Status status, EvidenceStrength strength, AssessmentConfidence confidence,
                           List<EvidenceReference> evidence, String rationale) {}

    /** The date is an explicit input so current jobs/credentials remain reproducible. */
    public RequirementAssessment assess(CurrentRequirement r, Response p, ReviewStatus review, LocalDate asOf) {
        Finding f;
        if (r.requirementKind() == RequirementKind.CONTEXTUAL) {
            f = new Finding(Status.NOT_APPLICABLE, EvidenceStrength.NONE, AssessmentConfidence.HIGH, List.of(), "Information de contexte, sans compétence exigée.");
        } else if (p == null) {
            f = unknown(List.of(), "Aucun profil candidat enregistré.");
        } else {
            f = switch (r.category()) {
                case TECH_SKILL, DOMAIN_KNOWLEDGE -> skill(r, p);
                case EXPERIENCE -> experience(r, p, asOf);
                case EDUCATION -> education(r, p);
                case LANGUAGE -> language(r, p);
                case CERTIFICATION -> certification(r, p, asOf);
                default -> unknown(List.of(), "Les données structurées disponibles ne permettent pas de conclure pour cette exigence.");
            };
        }
        EligibilityEffect effect = EligibilityEffect.NONE;
        if (Boolean.TRUE.equals(r.hardBlockerCandidate()) && r.requirementKind() == RequirementKind.REQUIRED
                && f.status != Status.MATCH && f.status != Status.NOT_APPLICABLE) {
            effect = EligibilityEffect.POSSIBLE_BLOCK;
            // A conditional blocker cannot be evaluated from its free-text condition.
            boolean explicitCondition = MatchingLabels.same(r.blockerCondition(), r.canonicalLabel());
            if (f.status == Status.MISSING && f.confidence == AssessmentConfidence.HIGH
                    && (review == ReviewStatus.CONFIRMED || review == ReviewStatus.CORRECTED)
                    && r.explicitness() == Explicitness.EXPLICIT && explicitCondition) effect = EligibilityEffect.BLOCK;
        }
        String attention = effect == EligibilityEffect.POSSIBLE_BLOCK
                ? "Vérifiez la condition bloquante et les informations du profil avant de conclure."
                : f.status == Status.UNKNOWN ? "Complétez ou vérifiez les informations pertinentes du profil." : null;
        boolean directEvidence = !f.evidence.isEmpty() && (f.status == Status.MATCH || f.status == Status.PARTIAL);
        return new RequirementAssessment(r.id(), f.status, directEvidence ? TransferRelation.EXACT : TransferRelation.NONE,
                f.strength, f.confidence, effect, f.evidence.stream().distinct().toList(), f.rationale, attention);
    }

    private List<EvidenceReference> skillEvidence(String label, Response p) {
        var skills = p.skills().stream().filter(s -> !MatchingLabels.skill(label).isEmpty()
                && MatchingLabels.skill(label).equals(MatchingLabels.skill(s.name()))).toList();
        Set<UUID> ids = new HashSet<>();
        List<EvidenceReference> evidence = new ArrayList<>();
        skills.forEach(s -> { ids.add(s.id()); evidence.add(new EvidenceReference(EvidenceType.PROFILE_SKILL, s.id(), s.name())); });
        p.experiences().stream().filter(e -> e.skillIds().stream().anyMatch(ids::contains)).forEach(e ->
                evidence.add(new EvidenceReference(EvidenceType.EXPERIENCE, e.id(), experienceLabel(e))));
        return evidence.stream().distinct().toList();
    }

    private Finding skill(CurrentRequirement r, Response p) {
        var evidence = skillEvidence(r.canonicalLabel(), p);
        if (r.constraint() != null) return unknown(evidence, "La compétence seule ne permet pas de vérifier la contrainte demandée.");
        if (evidence.isEmpty()) return unknown(evidence, "Aucune compétence explicite correspondante ; la liste ne prouve pas une absence de compétence.");
        boolean demonstrated = evidence.stream().anyMatch(e -> e.type() == EvidenceType.EXPERIENCE);
        return new Finding(Status.MATCH, demonstrated ? EvidenceStrength.STRONG : EvidenceStrength.MODERATE,
                AssessmentConfidence.HIGH, evidence, demonstrated ? "Compétence déclarée et reliée à une expérience." : "Compétence explicitement déclarée dans le profil.");
    }

    private Finding experience(CurrentRequirement r, Response p, LocalDate asOf) {
        var evidence = skillEvidence(r.canonicalLabel(), p);
        var c = r.constraint();
        if (c == null || c.operator() != ConstraintOperator.AT_LEAST
                || !Set.of("years", "year", "ans", "an").contains(MatchingLabels.text(c.unit()))
                || !c.value().matches("[0-9]+(?:\\.[0-9]+)?"))
            return unknown(evidence, "Durée minimale ou compétence concernée insuffisamment structurée.");
        double years = Double.parseDouble(c.value());
        if (!Double.isFinite(years) || years <= 0 || years > 100) return unknown(evidence, "Durée demandée non interprétable.");
        Set<UUID> relevant = new HashSet<>();
        evidence.stream().filter(e -> e.type() == EvidenceType.EXPERIENCE).forEach(e -> relevant.add(e.id()));
        record Interval(LocalDate start, LocalDate end) {}
        var intervals = new ArrayList<Interval>();
        for (var e : p.experiences()) {
            if (!relevant.contains(e.id())) continue;
            LocalDate end = e.current() ? asOf : e.endDate();
            if (e.startDate() == null || end == null || end.isAfter(asOf) || !end.isAfter(e.startDate())) continue;
            intervals.add(new Interval(e.startDate(), end));
        }
        if (intervals.isEmpty()) return unknown(evidence, "Aucune durée fiable d’expérience reliée à cette compétence.");
        intervals.sort(Comparator.comparing(Interval::start));
        long days = 0;
        LocalDate start = intervals.getFirst().start(), end = intervals.getFirst().end();
        for (var interval : intervals.subList(1, intervals.size())) {
            if (!interval.start().isAfter(end)) { if (interval.end().isAfter(end)) end = interval.end(); }
            else { days += ChronoUnit.DAYS.between(start, end); start = interval.start(); end = interval.end(); }
        }
        days += ChronoUnit.DAYS.between(start, end);
        // Whole-job periods are a documented proxy, not measured time spent on a technology.
        double demonstrated = days / 365.2425;
        if (demonstrated + 0.003 >= years) return new Finding(Status.MATCH, EvidenceStrength.STRONG,
                AssessmentConfidence.MEDIUM, evidence, "Les périodes d’expériences liées couvrent la durée minimale, sans compter deux fois les chevauchements. La durée d’usage effectif reste à confirmer.");
        if (demonstrated + 1.003 >= years) return new Finding(Status.PARTIAL, EvidenceStrength.MODERATE,
                AssessmentConfidence.MEDIUM, evidence, "Les périodes d’expériences liées sont environ un an en dessous du minimum demandé ; ce décalage ne suffit pas à conclure à un blocage.");
        return unknown(evidence, "Les périodes documentées sont inférieures au minimum ; le profil ne garantit pas un historique complet.");
    }

    private Finding education(CurrentRequirement r, Response p) {
        var evidence = p.educations().stream().filter(e -> MatchingLabels.same(e.degree(), r.canonicalLabel()))
                .map(e -> new EvidenceReference(EvidenceType.EDUCATION, e.id(), e.degree())).toList();
        if (r.constraint() != null || evidence.isEmpty()) return unknown(evidence, "Aucun diplôme directement comparable ; aucune équivalence de niveau n’est déduite.");
        return new Finding(Status.MATCH, EvidenceStrength.STRONG, AssessmentConfidence.HIGH, evidence, "Le diplôme déclaré porte exactement le libellé demandé.");
    }

    private Finding language(CurrentRequirement r, Response p) {
        var languages = p.languages().stream().filter(l -> !MatchingLabels.language(r.canonicalLabel()).isEmpty()
                && MatchingLabels.language(l.name()).equals(MatchingLabels.language(r.canonicalLabel()))).toList();
        var evidence = languages.stream().map(l -> new EvidenceReference(EvidenceType.LANGUAGE, l.id(), l.name() + " — " + Objects.toString(l.level(), "niveau inconnu"))).toList();
        var c = r.constraint();
        if (languages.size() != 1 || c == null || c.operator() != ConstraintOperator.AT_LEAST
                || !(c.unit() == null || Set.of("cefr", "cecrl").contains(MatchingLabels.text(c.unit())))
                || MatchingLabels.level(c.value()) < 0 || MatchingLabels.level(languages.getFirst().level()) < 0)
            return unknown(evidence, "Seuls des niveaux CECRL explicites A1 à C2 et un minimum structuré sont comparables.");
        boolean sufficient = MatchingLabels.level(languages.getFirst().level()) >= MatchingLabels.level(c.value());
        return new Finding(sufficient ? Status.MATCH : Status.MISSING, EvidenceStrength.STRONG, AssessmentConfidence.HIGH,
                evidence, sufficient ? "Le niveau CECRL déclaré atteint le minimum demandé." : "Le niveau CECRL déclaré est inférieur au minimum demandé.");
    }

    private Finding certification(CurrentRequirement r, Response p, LocalDate asOf) {
        var certificates = p.certifications().stream().filter(c -> MatchingLabels.same(c.name(), r.canonicalLabel())).toList();
        var evidence = certificates.stream().map(c -> new EvidenceReference(EvidenceType.CERTIFICATION, c.id(), c.name())).toList();
        if (r.constraint() != null || r.explicitness() != Explicitness.EXPLICIT
                || r.canonicalLabel().matches("(?i).*(\\bor\\b|\\bou\\b|/|\\bcertifications?\\b$).*"))
            return unknown(evidence, "Certification ou condition ambiguë : vérifier le titre exact attendu.");
        if (certificates.stream().anyMatch(c -> (c.issueDate() == null || !c.issueDate().isAfter(asOf))
                && (c.expirationDate() == null || !c.expirationDate().isBefore(asOf))))
            return new Finding(Status.MATCH, EvidenceStrength.STRONG, AssessmentConfidence.HIGH, evidence, "Certification exacte déclarée, sans expiration connue à la date d’évaluation.");
        if (!certificates.isEmpty()) return unknown(evidence, "La certification déclarée est expirée ou datée dans le futur ; sa validité actuelle doit être vérifiée.");
        if (p.certificationsComplete()) return new Finding(Status.MISSING, EvidenceStrength.STRONG, AssessmentConfidence.HIGH,
                List.of(new EvidenceReference(EvidenceType.CERTIFICATION_LIST, p.id(), "Liste déclarée complète dans le profil")),
                "Certification absente de la liste explicitement déclarée complète par le candidat.");
        return unknown(evidence, "Certification non renseignée ; la liste n’est pas déclarée complète.");
    }

    private static String experienceLabel(ExperienceData e) {
        return Objects.toString(e.company(), "Entreprise non renseignée") + " — " + Objects.toString(e.position(), "Poste non renseigné");
    }
    private static Finding unknown(List<EvidenceReference> evidence, String rationale) {
        return new Finding(Status.UNKNOWN, evidence.isEmpty() ? EvidenceStrength.NONE : EvidenceStrength.WEAK,
                AssessmentConfidence.LOW, evidence, rationale);
    }
}
