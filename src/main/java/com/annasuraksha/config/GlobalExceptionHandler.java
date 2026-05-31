package com.annasuraksha.config;

import com.annasuraksha.model.api.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException e) {
        return ApiResponse.error("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbidden(AccessDeniedException e) {
        return ApiResponse.error("FORBIDDEN", "You do not have permission to access this resource.");
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(jakarta.persistence.EntityNotFoundException e) {
        return ApiResponse.error("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        return ApiResponse.error("INTERNAL_ERROR", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResponse.error("INTERNAL_ERROR", "An internal error occurred.");
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
}
