package io.github.roledock.joboffer.extraction;

public interface JobOfferExtractor {
    /** Returns a validated extraction, or throws ExtractionException; never returns null. */
    JobOfferExtraction extract(String rawJobOffer);
}
