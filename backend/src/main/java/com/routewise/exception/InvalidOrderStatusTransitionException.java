package com.routewise.exception;

/** Thrown when a status change doesn't follow the order state machine (see OrderServiceImpl). */
public class InvalidOrderStatusTransitionException extends RuntimeException {

  public InvalidOrderStatusTransitionException(String message) {
    super(message);
  }
}
