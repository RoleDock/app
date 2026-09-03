package io.github.roledock.api.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String INVALID_REQUEST_MESSAGE = "La requête contient des données invalides.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return invalid(INVALID_REQUEST_MESSAGE, fields);
    }

    @ExceptionHandler(ApiValidationException.class)
    ResponseEntity<ApiError> apiValidation(ApiValidationException exception) {
        return invalid(exception.getMessage(), exception.getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadablePayload() {
        return invalid(INVALID_REQUEST_MESSAGE, Map.of("request", "Le format de la requête est invalide."));
    }

    private ResponseEntity<ApiError> invalid(String message, Map<String, String> fields) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(VALIDATION_ERROR, message, fields));
    }
}
