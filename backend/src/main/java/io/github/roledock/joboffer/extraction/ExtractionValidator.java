package io.github.roledock.joboffer.extraction;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;

@Component
public class ExtractionValidator {
    private final Validator validator;

    public ExtractionValidator(Validator validator) { this.validator = validator; }

    public JobOfferExtraction validate(JobOfferExtraction result, String source) {
        if (result == null || !validator.validate(result).isEmpty()) {
            throw ExtractionException.failed(ExtractionException.Reason.INVALID_FIELDS);
        }
        for (var requirement : result.requirements()) {
            if (!source.contains(requirement.rawText())) {
                throw ExtractionException.failed(ExtractionException.Reason.QUOTE_MISMATCH);
            }
            if (requirement.hardBlockerCandidate()) {
                if (requirement.explicitness() != Explicitness.EXPLICIT
                        || requirement.requirementKind() != RequirementKind.REQUIRED
                        || requirement.blockerCondition() == null
                        || requirement.blockerCondition().isBlank()) {
                    throw ExtractionException.failed(ExtractionException.Reason.BLOCKER_INCONSISTENT);
                }
            } else if (requirement.blockerCondition() != null) {
                throw ExtractionException.failed(ExtractionException.Reason.BLOCKER_INCONSISTENT);
            }
        }
        return result;
    }
}
