package com.energymonitor.home.adapter.in.web;

import com.energymonitor.home.application.exception.AccessCodeNotFoundException;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.exception.MissingUserIdentityException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the home module's web layer.
 *
 * <p>Maps application exceptions to HTTP status codes:
 * <ul>
 *   <li>{@link MissingUserIdentityException} → 401 Unauthorized</li>
 *   <li>{@link HomeNotFoundException} → 404 Not Found</li>
 *   <li>{@link HomeTypeNotFoundException} → 404 Not Found</li>
 *   <li>{@link UserHomeNotFoundException} → 404 Not Found</li>
 *   <li>{@link AccessCodeNotFoundException} → 404 Not Found</li>
 *   <li>{@link HomeAccessDeniedException} → 403 Forbidden</li>
 *   <li>{@link HomeConflictException} → 409 Conflict</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "com.energymonitor.home.adapter.in.web")
public class HomeExceptionHandler {

    @ExceptionHandler(MissingUserIdentityException.class)
    public ResponseEntity<Map<String, String>> handleMissingUserIdentity(MissingUserIdentityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HomeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleHomeNotFound(HomeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HomeTypeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleHomeTypeNotFound(HomeTypeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UserHomeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserHomeNotFound(UserHomeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AccessCodeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAccessCodeNotFound(AccessCodeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HomeAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleHomeAccessDenied(HomeAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HomeConflictException.class)
    public ResponseEntity<Map<String, String>> handleHomeConflict(HomeConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
