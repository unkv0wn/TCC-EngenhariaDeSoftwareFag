package com.routewise.dto;

/**
 * Wrapper for all Server-Sent Events published on the SSE channel.
 *
 * <p>Event types:
 * <ul>
 *   <li>{@code PROCESSING} — computation has started; payload is null.</li>
 *   <li>{@code COMPLETED}  — computation succeeded; payload is a
 *       {@link RouteResultDto}.</li>
 *   <li>{@code ERROR}      — computation failed; payload is an
 *       {@link ErrorPayload}.</li>
 * </ul>
 *
 * @param type       one of PROCESSING | COMPLETED | ERROR
 * @param requestId  correlation identifier
 * @param payload    event-specific data object (null for PROCESSING)
 */
public record SseEventDto(String type, String requestId, Object payload) {

  // ── Factory methods ───────────────────────────────────────────────────────

  public static SseEventDto processing(String requestId) {
    return new SseEventDto("PROCESSING", requestId, null);
  }

  public static SseEventDto completed(String requestId, RouteResultDto result) {
    return new SseEventDto("COMPLETED", requestId, result);
  }

  public static SseEventDto error(String requestId, String message) {
    return new SseEventDto("ERROR", requestId, new ErrorPayload(message));
  }

  // ── Nested types ─────────────────────────────────────────────────────────

  /** Payload for ERROR events. */
  public record ErrorPayload(String message) {}
}
