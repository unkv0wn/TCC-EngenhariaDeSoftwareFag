package com.routewise.exception;

/** Thrown when a route status change doesn't follow the route state machine (see SavedRouteServiceImpl). */
public class InvalidRouteStatusTransitionException extends RuntimeException {

  public InvalidRouteStatusTransitionException(String message) {
    super(message);
  }
}
