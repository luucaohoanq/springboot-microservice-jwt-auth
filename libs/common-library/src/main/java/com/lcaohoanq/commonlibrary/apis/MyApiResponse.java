
package com.lcaohoanq.commonlibrary.apis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse.Error;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse.Success;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse.ValidationError;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MyApiResponse.Success.class, name = "success"),
    @JsonSubTypes.Type(value = MyApiResponse.Error.class, name = "error"),
    @JsonSubTypes.Type(value = MyApiResponse.ValidationError.class, name = "validation_error")
})
public sealed interface MyApiResponse<T> permits Error, Success, ValidationError {

  int getStatusCode();

  String getMessage();

  Instant getTimestamp();

  // ✅ Success record
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Success<T>(
      @JsonIgnore int statusCode, String message, T data, @JsonIgnore Instant timestamp)
      implements MyApiResponse<T> {
    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }
  }

  // ✅ Enhanced Error record with path tracking
  record Error<T>(int statusCode, String message, String reason, String path, Instant timestamp)
      implements MyApiResponse<T> {
    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }
  }

  // ✅ Enhanced ValidationError record with path tracking
  record ValidationError<T>(
      int statusCode,
      String message,
      Map<String, String> fieldErrors,
      String path,
      Instant timestamp)
      implements MyApiResponse<T> {
    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }
  }

  // ✅ Helper method to get current request path
  private static String getCurrentPath() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      return attributes.getRequest().getRequestURI();
    } catch (IllegalStateException e) {
      return "unknown";
    }
  }

  // ✅ Helper methods (updated to include path)
  static <T> ResponseEntity<MyApiResponse<T>> success(T data) {
    return ResponseEntity.ok(new Success<>(200, "Success", data, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> success() {
    return ResponseEntity.ok(new Success<>(200, "Success", null, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> created(T data) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new Success<>(201, "Created successfully", data, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> created() {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new Success<>(201, "Created successfully", null, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> updated(T data) {
    return ResponseEntity.ok(new Success<>(200, "Updated successfully", data, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> updated() {
    return ResponseEntity.ok(new Success<>(200, "Updated successfully", null, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> badRequest(String reason) {
    return ResponseEntity.badRequest()
        .body(
            new Error<>(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                reason,
                getCurrentPath(),
                Instant.now()));
  }

  static ResponseEntity<MyApiResponse<Object>> validationError(Map<String, String> errors) {
    return ResponseEntity.badRequest()
        .body(
            new ValidationError<>(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors,
                getCurrentPath(),
                Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> notFound(String reason) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new Error<>(404, "Not Found", reason, getCurrentPath(), Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> noContent() {
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(new Success<>(204, "No Content", null, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> error(
      HttpStatus status, String message, String reason) {
    return ResponseEntity.status(status)
        .body(new Error<>(status.value(), message, reason, getCurrentPath(), Instant.now()));
  }

  // ✅ Overloaded methods for explicit path specification
  static <T> ResponseEntity<MyApiResponse<T>> badRequest(String reason, String path) {
    return ResponseEntity.badRequest()
        .body(
            new Error<>(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", reason, path, Instant.now()));
  }

  static ResponseEntity<MyApiResponse<Object>> validationError(
      Map<String, String> errors, String path) {
    return ResponseEntity.badRequest()
        .body(
            new ValidationError<>(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", errors, path, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> notFound(String reason, String path) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new Error<>(404, "Not Found", reason, path, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> error(
      HttpStatus status, String message, String reason, String path) {
    return ResponseEntity.status(status)
        .body(new Error<>(status.value(), message, reason, path, Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> serverError(String reason) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Error<>(500, "Internal Server Error", reason, getCurrentPath(), Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> unauthorized(String reason) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new Error<>(401, "Unauthorized", reason, getCurrentPath(), Instant.now()));
  }

  static <T> ResponseEntity<MyApiResponse<T>> forbidden(String reason) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new Error<>(403, "Forbidden", reason, getCurrentPath(), Instant.now()));
  }
}
