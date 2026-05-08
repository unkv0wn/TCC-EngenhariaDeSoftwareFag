package com.routewise.exception;

/**
 * Thrown when a call to the OSRM routing API fails or returns an unexpected
 * response code.
 *
 * <p>This exception is handled with graceful degradation in the
 * {@link com.routewise.service.impl.OsrmClientServiceImpl}: if OSRM is
 * unavailable, the A* result is still returned without OSRM validation data.
 */
public class OsrmClientException extends RuntimeException {

  private final int httpStatus;

  public OsrmClientException(String message) {
    super(message);
    this.httpStatus = 0;
  }

  public OsrmClientException(String message, int httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public OsrmClientException(String message, Throwable cause) {
    super(message, cause);
    this.httpStatus = 0;
  }

  public int getHttpStatus() {
    return httpStatus;
  }
}
