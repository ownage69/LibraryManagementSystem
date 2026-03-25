package com.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request, exception);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                request,
                exception
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessConflict(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();
        return buildValidationErrorResponse(errors, request, exception);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ValidationError> errors = exception.getAllValidationResults()
                .stream()
                .flatMap(result -> {
                    String parameterName = resolveParameterName(
                            result.getMethodParameter().getParameterName()
                    );
                    return result.getResolvableErrors()
                            .stream()
                            .map(error -> new ApiErrorResponse.ValidationError(
                                    parameterName,
                                    result.getArgument(),
                                    resolveMessage(error)
                            ));
                })
                .toList();
        return buildValidationErrorResponse(errors, request, exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ValidationError> errors = exception.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .toList();
        return buildValidationErrorResponse(errors, request, exception);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value '%s' for parameter '%s'".formatted(
                exception.getValue(),
                exception.getName()
        );
        return build(HttpStatus.BAD_REQUEST, message, request, exception);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or malformed",
                request,
                exception
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                request,
                exception
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception while processing {}",
                request.getRequestURI(),
                exception
        );
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Unexpected server error")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ApiErrorResponse> buildValidationErrorResponse(
            List<ApiErrorResponse.ValidationError> errors,
            HttpServletRequest request,
            Exception exception
    ) {
        String message = errors.isEmpty() ? "Validation failed" : errors.get(0).getMessage();
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errors(errors)
                .build();
        logHandledException(HttpStatus.BAD_REQUEST, request, exception);
        return ResponseEntity.badRequest().body(body);
    }

    private ApiErrorResponse.ValidationError mapFieldError(FieldError fieldError) {
        return new ApiErrorResponse.ValidationError(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                resolveMessage(fieldError)
        );
    }

    private ApiErrorResponse.ValidationError mapConstraintViolation(
            ConstraintViolation<?> violation
    ) {
        return new ApiErrorResponse.ValidationError(
                extractLeafNode(violation.getPropertyPath().toString()),
                violation.getInvalidValue(),
                violation.getMessage()
        );
    }

    private String resolveMessage(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message == null ? "Validation failed" : message;
    }

    private String resolveParameterName(String parameterName) {
        return parameterName == null || parameterName.isBlank() ? "parameter" : parameterName;
    }

    private String extractLeafNode(String propertyPath) {
        int separatorIndex = propertyPath.lastIndexOf('.');
        if (separatorIndex < 0) {
            return propertyPath;
        }
        return propertyPath.substring(separatorIndex + 1);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return build(status, message, request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Exception exception
    ) {
        if (exception != null) {
            logHandledException(status, request, exception);
        }
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private void logHandledException(
            HttpStatus status,
            HttpServletRequest request,
            Exception exception
    ) {
        String message = "{} on {}: {}";
        if (status.is5xxServerError()) {
            log.error(
                    message,
                    status.getReasonPhrase(),
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception
            );
            return;
        }
        log.warn(
                message,
                status.getReasonPhrase(),
                request.getRequestURI(),
                exception.getMessage()
        );
    }
}
