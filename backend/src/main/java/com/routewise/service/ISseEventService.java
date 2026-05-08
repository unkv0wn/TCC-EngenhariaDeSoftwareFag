package com.routewise.service;

import com.routewise.dto.SseEventDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Contract for the Server-Sent Events lifecycle manager.
 *
 * <p>Manages a map of in-flight {@link SseEmitter} instances keyed by
 * {@code requestId} and provides thread-safe helpers to create, emit to,
 * and close emitters.
 */
public interface ISseEventService {

  /**
   * Creates a new {@link SseEmitter} for the given {@code requestId},
   * registers it in the in-flight map, and registers cleanup callbacks for
   * timeout/completion/error. Automatically sends a connection-acknowledgement
   * comment to keep the stream alive.
   *
   * @param requestId unique correlation ID
   * @return the newly created and registered emitter
   */
  SseEmitter createEmitter(String requestId);

  /**
   * Serialises {@code event} as JSON and sends it to the emitter identified
   * by {@code requestId}.  If the emitter is not found (e.g., client already
   * disconnected), this method is a no-op.
   *
   * @param requestId correlation ID
   * @param event     event to send
   */
  void emit(String requestId, SseEventDto event);

  /**
   * Completes the emitter normally, closing the SSE stream on the client side.
   *
   * @param requestId correlation ID
   */
  void complete(String requestId);

  /**
   * Completes the emitter with an error, signalling the client that the stream
   * has ended abnormally.
   *
   * @param requestId correlation ID
   * @param throwable the error that caused the failure
   */
  void completeWithError(String requestId, Throwable throwable);
}
