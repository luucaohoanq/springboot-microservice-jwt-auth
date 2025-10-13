package com.lcaohoanq.authserver.exceptions;

import com.lcaohoanq.commonlibrary.apis.ApiResponse;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.exceptions.AccountNotActivatedException;
import com.lcaohoanq.commonlibrary.exceptions.AccountResourceException;
import com.lcaohoanq.commonlibrary.exceptions.ConflictException;
import com.lcaohoanq.commonlibrary.exceptions.NotFoundException;
import feign.FeignException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotActivatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<MyApiResponse<String>> handleAccountNotActivated(AccountNotActivatedException ex) {
        log.warn("Account not activated: {}", ex.getMessage());
        return MyApiResponse.unauthorized(ex.getMessage());
    }

    @ExceptionHandler(AccountResourceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<MyApiResponse<String>> handleAccountResourceException(AccountResourceException ex) {
        log.error("Account resource error: {}", ex.getMessage());
        return MyApiResponse.badRequest(ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<MyApiResponse<String>> handleNotFoundException(NotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return MyApiResponse.notFound(ex.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<MyApiResponse<String>> handleFeignException(FeignException ex) {
        log.error("Feign client error: {}", ex.getMessage());

        if (ex.status() == 404) {
            return MyApiResponse.notFound("Resource not found in user service");
        } else if (ex.status() == 401) {
            return MyApiResponse.unauthorized("Unauthorized access to user service");
        } else if (ex.status() == 400) {
            return MyApiResponse.badRequest("Bad request to user service");
        }

        return MyApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, "User service unavailable");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
        List<String> errorList =
            ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        String errors = String.join("; ", errorList);

        ApiResponse<Object> response =
            new ApiResponse<>(HttpStatus.BAD_REQUEST.toString(), errors, null, "VALIDATION_ERROR");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle validation errors from BindException (happens with @ModelAttribute validation)
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<MyApiResponse<Object>> handleBindExceptions(
        BindException ex, WebRequest request) {
        log.error("Binding error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return MyApiResponse.validationError(errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MyApiResponse<Object>> handleBadCredentialsException(
        BadCredentialsException ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return MyApiResponse.error(
            HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<MyApiResponse<String>> handleConflictException(ConflictException ex) {
        log.error("Conflict error: {}", ex.getMessage());
        return MyApiResponse.error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<MyApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return MyApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<MyApiResponse<String>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return MyApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}