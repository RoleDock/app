package io.github.roledock.joboffer;

import io.github.roledock.api.error.ApiError;
import io.github.roledock.joboffer.extraction.ExtractionException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static io.github.roledock.joboffer.JobOfferDtos.*;

@RestController
@RequestMapping("/api/job-offers")
class JobOfferController {
    private static final String PENDING = JobOfferController.class.getName() + ".pending";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobOfferController.class);
    private final JobOfferService service;
    private final JobOfferReviewService reviewService;

    JobOfferController(JobOfferService service, JobOfferReviewService reviewService) {
        this.service = service; this.reviewService = reviewService;
    }

    @PutMapping("/{id}/review")
    Response review(@PathVariable UUID id, @Valid @RequestBody JobOfferReviewDtos.Command command) {
        return reviewService.review(id, command);
    }

    @PostMapping("/analyze")
    Analysis analyze(@Valid @RequestBody AnalyzeRequest request, HttpSession session) {
        var pending = service.analyze(request);
        // One pending analysis per browser session; no offer row exists before Save.
        synchronized (session) { session.setAttribute(PENDING, pending); }
        return new Analysis(pending.id(), pending.analyzedAt(), pending.extraction());
    }

    @PostMapping
    ResponseEntity<?> save(@Valid @RequestBody SaveRequest request, HttpSession session) {
        synchronized (session) {
            var pending = (JobOfferService.Pending) session.getAttribute(PENDING);
            if (pending == null || !pending.id().equals(request.analysisId())) {
                return ResponseEntity.status(409).body(new ApiError("ANALYSIS_EXPIRED",
                        "Cette analyse a expiré ou a été remplacée. Analysez à nouveau votre annonce.", Map.of()));
            }
            var result = service.save(pending);
            return ResponseEntity.created(URI.create("/api/job-offers/" + result.id())).body(result);
        }
    }

    @GetMapping("/{id}")
    ResponseEntity<?> get(@PathVariable UUID id) {
        return service.get(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() ->
                ResponseEntity.status(404).body(new ApiError("OFFER_NOT_FOUND", "Offre introuvable.", Map.of())));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<ApiError> reviewFailure(org.springframework.web.server.ResponseStatusException exception) {
        boolean missing = exception.getStatusCode().value() == 404;
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(
                missing ? "OFFER_NOT_FOUND" : "ALREADY_REVIEWED",
                missing ? "Offre introuvable." : "Cette analyse a déjà été vérifiée.", Map.of()));
    }

    @ExceptionHandler(ExtractionException.class)
    ResponseEntity<ApiError> extractionFailure(ExtractionException exception) {
        log.warn("Job-offer extraction failed: reason={} providerStatus={}", exception.reason(), exception.providerStatus());
        return ResponseEntity.status(exception.isUnavailable() ? 503 : 502)
                .body(new ApiError("EXTRACTION_FAILED", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler({org.springframework.dao.DataAccessException.class,
            jakarta.persistence.PersistenceException.class, org.springframework.transaction.TransactionException.class})
    ResponseEntity<ApiError> persistenceFailure() {
        return ResponseEntity.internalServerError().body(new ApiError("PERSISTENCE_FAILED",
                "L’offre n’a pas pu être enregistrée. Réessayez.", Map.of()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidId() {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Identifiant invalide.", Map.of()));
    }
}
