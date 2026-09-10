package io.github.roledock.joboffer;

import static io.github.roledock.joboffer.JobOfferReviewDtos.*;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import io.github.roledock.api.error.ApiValidationException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class JobOfferReviewService {
    private final EntityManager entityManager;
    JobOfferReviewService(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional
    JobOfferDtos.Response review(UUID id, Command command) {
        // Serialize decisions on the aggregate, including changes to its ordered children.
        var offer = entityManager.find(JobOffer.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (offer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable.");
        if (command.action() == Action.BYPASS) {
            if (command.extraction() != null) invalid("extraction", "La poursuite sans vérification ne peut pas enregistrer des modifications.");
            if (offer.reviewStatus != ReviewStatus.UNREVIEWED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette analyse a déjà été vérifiée.");
            if (offer.reviewBypassedAt == null) offer.reviewBypassedAt = Instant.now();
        } else {
            var data = command.extraction();
            if (data == null) invalid("extraction", "L’analyse à vérifier est requise.");
            if (!Objects.equals(data.sourceLanguage(), offer.sourceLanguage))
                invalid("sourceLanguage", "La langue de l’extraction initiale est conservée.");
            var existing = new HashMap<UUID, JobRequirement>();
            offer.requirements.forEach(r -> existing.put(r.id, r));
            var seen = new HashSet<UUID>();
            for (int i = 0; i < data.requirements().size(); i++) {
                var r = data.requirements().get(i);
                String field = "requirements[" + i + "]";
                if (r.id() != null) {
                    var original = existing.get(r.id());
                    if (original == null || !seen.add(r.id())) invalid(field, "Identifiant d’exigence inconnu ou répété.");
                    if (!Objects.equals(r.rawText(), original.rawText) || r.source() != original.source
                            || r.extractionConfidence() != original.extractionConfidence)
                        invalid(field, "La citation et la provenance ne peuvent pas être modifiées.");
                } else if (r.rawText() != null || r.extractionConfidence() != null
                        || (r.source() != null && r.source() != RequirementSource.USER_ADDED)) {
                    invalid(field, "Une exigence manuelle ne possède pas de citation ni de confiance d’extraction.");
                }
                if (r.hardBlockerCandidate()) {
                    if (r.requirementKind() != RequirementKind.REQUIRED || r.explicitness() != Explicitness.EXPLICIT
                            || r.blockerCondition() == null || r.blockerCondition().isBlank())
                        invalid(field + ".blockerCondition", "Un blocage potentiel doit être requis, explicite et accompagné d’une condition.");
                } else if (r.blockerCondition() != null) invalid(field + ".blockerCondition", "Retirez la condition lorsque le blocage est désactivé.");
            }
            boolean changed = !offer.toResponse().extraction().equals(data);
            if (changed) {
                offer.company = data.company(); offer.position = data.position(); offer.summary = data.summary();
                offer.city = data.location().city(); offer.region = data.location().region(); offer.country = data.location().country();
                offer.contractType = data.contractType(); offer.workArrangementType = data.workArrangement().type();
                offer.remoteArea = data.workArrangement().remoteArea(); offer.onSiteDaysPerWeek = data.workArrangement().onSiteDaysPerWeek();
                offer.missions.clear(); offer.missions.addAll(data.missions());
                // Move surviving rows beyond both order ranges before assigning final positions.
                // This avoids transient unique-key collisions on PostgreSQL when deleting/reordering.
                int offset = offer.requirements.size() + data.requirements().size();
                offer.requirements.removeIf(r -> !seen.contains(r.id));
                for (var r : offer.requirements) r.sortOrder += offset;
                entityManager.flush();
                for (int i = 0; i < data.requirements().size(); i++) {
                    var r = data.requirements().get(i);
                    var target = r.id() == null ? new JobRequirement() : existing.get(r.id());
                    if (r.id() == null) {
                        target.id = UUID.randomUUID(); target.offer = offer; target.source = RequirementSource.USER_ADDED;
                        offer.requirements.add(target);
                    }
                    target.sortOrder = i; target.canonicalLabel = r.canonicalLabel(); target.category = r.category();
                    target.requirementKind = r.requirementKind(); target.centrality = r.centrality(); target.explicitness = r.explicitness();
                    target.hardBlockerCandidate = r.hardBlockerCandidate(); target.blockerCondition = r.blockerCondition();
                    target.constraintOperator = r.constraint() == null ? null : r.constraint().operator();
                    target.constraintValue = r.constraint() == null ? null : r.constraint().value();
                    target.constraintUnit = r.constraint() == null ? null : r.constraint().unit();
                }
                offer.requirements.sort(Comparator.comparingInt(r -> r.sortOrder));
                offer.reviewStatus = ReviewStatus.CORRECTED;
            } else if (offer.reviewStatus != ReviewStatus.CORRECTED) offer.reviewStatus = ReviewStatus.CONFIRMED;
        }
        entityManager.flush();
        return offer.toResponse();
    }
    private static void invalid(String field, String message) {
        throw new ApiValidationException("La vérification n’a pas été enregistrée.", Map.of(field, message));
    }
}
