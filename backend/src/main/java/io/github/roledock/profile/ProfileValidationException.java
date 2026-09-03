package io.github.roledock.profile;

import java.util.Map;

class ProfileValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    ProfileValidationException(Map<String, String> fieldErrors) {
        super("Le profil contient des données invalides.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
