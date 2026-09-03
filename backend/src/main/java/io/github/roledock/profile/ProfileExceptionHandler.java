package io.github.roledock.profile;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProfileController.class)
class ProfileExceptionHandler {

    record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return invalid(fields);
    }

    @ExceptionHandler(ProfileValidationException.class)
    ResponseEntity<ApiError> businessValidation(ProfileValidationException exception) {
        return invalid(exception.getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadablePayload() {
        return invalid(Map.of("request", "Le format de la requête est invalide."));
    }

    private ResponseEntity<ApiError> invalid(Map<String, String> fields) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", "Le profil contient des données invalides.", fields));
    }
}
