package io.github.roledock.joboffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.roledock.api.error.ApiValidationException;
import io.github.roledock.joboffer.extraction.*;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static io.github.roledock.joboffer.JobOfferDtos.*;

@Service
class JobOfferService {
    record Pending(UUID id, String originalText, String sourceUrl, Instant analyzedAt,
                   JobOfferExtraction extraction) {}
    private final JobOfferExtractor extractor;
    private final ExtractionValidator validator;
    private final ObjectMapper mapper;
    private final JobOfferRepository repository;
    private final EntityManager entityManager;

    JobOfferService(JobOfferExtractor extractor, ExtractionValidator validator, ObjectMapper mapper,
                    JobOfferRepository repository, EntityManager entityManager) {
        this.extractor = extractor;
        this.validator = validator;
        this.mapper = mapper;
        this.repository = repository;
        this.entityManager = entityManager;
    }

    Pending analyze(AnalyzeRequest request) {
        String url = request.sourceUrl();
        if (url != null && url.isBlank()) url = null;
        if (url != null) {
            try {
                URI uri = URI.create(url);
                if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                        || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException exception) {
                throw new ApiValidationException("L’URL source doit être une URL HTTP(S) valide.",
                        Map.of("sourceUrl", "URL invalide."));
            }
        }
        var extraction = validator.validate(extractor.extract(request.originalText()), request.originalText());
        return new Pending(UUID.randomUUID(), request.originalText(), url,
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), extraction);
    }

    @Transactional
    Response save(Pending pending) {
        // The server-generated analysis ID also makes retries idempotent.
        var existing = repository.findById(pending.id());
        if (existing.isPresent()) return existing.get().toResponse();
        final String snapshot;
        try {
            snapshot = mapper.writeValueAsString(pending.extraction());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize validated extraction.");
        }
        var offer = new JobOffer(pending.id(), pending.originalText(), pending.sourceUrl(),
                pending.analyzedAt(), pending.extraction(), snapshot);
        entityManager.persist(offer);
        entityManager.flush();
        return offer.toResponse();
    }

    @Transactional(readOnly = true)
    Optional<Response> get(UUID id) {
        return repository.findById(id).map(JobOffer::toResponse);
    }
}
