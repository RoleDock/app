package io.github.roledock.api.error;

import java.util.Map;

public final class ApiValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ApiValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
