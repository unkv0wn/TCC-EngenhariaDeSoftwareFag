package com.routewise.exception;

/**
 * Thrown when the A* optimizer or OSRM client cannot produce a valid route.
 *
 * <p>This is an unchecked exception. The {@link
 * com.routewise.exception.GlobalExceptionHandler} maps it to an SSE ERROR
 * event and an HTTP 422 response for direct REST callers.
 */
public class RouteComputationException extends RuntimeException {

  public RouteComputationException(String message) {
    super(message);
  }

  public RouteComputationException(String message, Throwable cause) {
    super(message, cause);
  }
}
