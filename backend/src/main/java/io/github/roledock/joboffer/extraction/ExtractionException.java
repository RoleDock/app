package io.github.roledock.joboffer.extraction;

public class ExtractionException extends RuntimeException {
    public enum Reason {
        NOT_CONFIGURED, PROVIDER_HTTP, TIMEOUT, TRANSPORT, PROVIDER_INCOMPLETE,
        PROVIDER_REFUSAL, PROVIDER_FORMAT, INVALID_JSON, INVALID_FIELDS,
        QUOTE_MISMATCH, BLOCKER_INCONSISTENT, UNEXPECTED
    }
    private final Reason reason;
    private final Integer providerStatus;

    private ExtractionException(Reason reason) { this(reason, null); }

    private ExtractionException(Reason reason, Integer providerStatus) {
        super(reason == Reason.NOT_CONFIGURED ? "Job-offer extraction is not configured." : "Job-offer extraction failed.");
        this.reason = reason;
        this.providerStatus = providerStatus;
    }

    public static ExtractionException unavailable() { return failed(Reason.NOT_CONFIGURED); }
    public static ExtractionException failed() { return failed(Reason.UNEXPECTED); }
    public static ExtractionException failed(Reason reason) { return new ExtractionException(reason); }
    public boolean isUnavailable() { return reason == Reason.NOT_CONFIGURED; }
    public static ExtractionException providerHttp(int status) {
        return new ExtractionException(Reason.PROVIDER_HTTP, status);
    }
    public Integer providerStatus() { return providerStatus; }
    public Reason reason() { return reason; }
}
