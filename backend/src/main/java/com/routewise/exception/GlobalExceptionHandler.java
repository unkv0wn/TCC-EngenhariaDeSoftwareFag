package com.routewise.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler.
 *
 * <p>Maps domain and validation exceptions to RFC 9457 Problem Detail responses
 * so that clients receive structured, machine-readable error payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ── Validation errors (DTO @Valid) ────────────────────────────────────────

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    String detail = ex.getBindingResult().getFieldErrors().stream()
      .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
      .collect(Collectors.joining("; "));

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Validation Error");
    problem.setDetail(detail);
    log.warn("Validation failed: {}", detail);
    return ResponseEntity.badRequest().body(problem);
  }

  // ── Constraint violations ─────────────────────────────────────────────────

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraints(ConstraintViolationException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Constraint Violation");
    problem.setDetail(ex.getMessage());
    log.warn("Constraint violation: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(problem);
  }

  // ── Unreadable request body ───────────────────────────────────────────────

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Malformed Request");
    problem.setDetail("Request body is invalid or missing: " + ex.getMessage());
    log.warn("Malformed request body: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(problem);
  }

  // ── Resource not found (lookup by id) ─────────────────────────────────────

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Resource Not Found");
    problem.setDetail(ex.getMessage());
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  // ── Unique constraint / FK violations ─────────────────────────────────────

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("Data Integrity Violation");
    problem.setDetail("Já existe um registro com esses dados, ou ele ainda está em uso por outro cadastro.");
    log.warn("Data integrity violation: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  // ── Route computation failure ─────────────────────────────────────────────

  @ExceptionHandler(RouteComputationException.class)
  public ResponseEntity<ProblemDetail> handleRouteComputation(RouteComputationException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setTitle("Route Computation Failed");
    problem.setDetail(ex.getMessage());
    log.error("Route computation failed", ex);
    return ResponseEntity.unprocessableEntity().body(problem);
  }

  // ── OSRM client failure (surfaced only on direct REST callers) ────────────

  @ExceptionHandler(OsrmClientException.class)
  public ResponseEntity<ProblemDetail> handleOsrmClient(OsrmClientException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
    problem.setTitle("Routing Engine Error");
    problem.setDetail("OSRM routing service returned an error: " + ex.getMessage());
    log.error("OSRM client error (status={})", ex.getHttpStatus(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
  }

  // ── Catch-all ────────────────────────────────────────────────────────────

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setTitle("Internal Server Error");
    problem.setDetail("An unexpected error occurred. Please try again.");
    log.error("Unexpected error", ex);
    return ResponseEntity.internalServerError().body(problem);
  }
}
