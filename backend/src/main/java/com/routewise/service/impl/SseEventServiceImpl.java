package com.routewise.service.impl;

import com.routewise.dto.SseEventDto;
import com.routewise.service.ISseEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSE lifecycle manager.
 *
 * <p>Maintains a thread-safe map of in-flight {@link SseEmitter} instances
 * keyed by {@code requestId}. Each emitter is registered with timeout,
 * completion, and error callbacks to guarantee clean resource release
 * regardless of how the stream ends.
 */
@Service
public class SseEventServiceImpl implements ISseEventService {

  private static final Logger log = LoggerFactory.getLogger(SseEventServiceImpl.class);

  @Value("${routewise.sse.timeout-ms:180000}")
  private long sseTimeoutMs;

  /** In-flight emitters: requestId → SseEmitter */
  private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  // ─────────────────────────────────────────────────────────────────────────
  // ISseEventService implementation
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  public SseEmitter createEmitter(String requestId) {
    SseEmitter emitter = new SseEmitter(sseTimeoutMs);

    // Register cleanup callbacks — called by Spring on any terminal event
    Runnable cleanup = () -> {
      SseEmitter removed = emitters.remove(requestId);
      if (removed != null) {
        log.debug("SSE emitter cleaned up: requestId={}", requestId);
      }
    };

    emitter.onCompletion(cleanup);
    emitter.onTimeout(() -> {
      log.warn("SSE emitter timed out: requestId={}", requestId);
      cleanup.run();
    });
    emitter.onError(ex -> {
      log.warn("SSE emitter error: requestId={}, error={}", requestId, ex.getMessage());
      cleanup.run();
    });

    emitters.put(requestId, emitter);
    log.debug("SSE emitter created: requestId={}, activEmitters={}", requestId, emitters.size());
    return emitter;
  }

  @Override
  public void emit(String requestId, SseEventDto event) {
    SseEmitter emitter = emitters.get(requestId);
    if (emitter == null) {
      log.warn("Cannot emit event: no emitter found for requestId={}", requestId);
      return;
    }

    try {
      emitter.send(
        SseEmitter.event()
          .name(Objects.requireNonNull(event.type()))
          .data(Objects.requireNonNull(event))
          .id(Objects.requireNonNull(requestId))
      );
      log.debug("SSE event sent: requestId={}, type={}", requestId, event.type());
    } catch (IOException ex) {
      log.warn(
        "Failed to send SSE event to requestId={}: {}; removing emitter",
        requestId, ex.getMessage()
      );
      emitters.remove(requestId);
    }
  }

  @Override
  public void complete(String requestId) {
    SseEmitter emitter = emitters.remove(requestId);
    if (emitter != null) {
      try {
        emitter.complete();
        log.debug("SSE emitter completed: requestId={}", requestId);
      } catch (Exception ex) {
        log.warn("Error completing SSE emitter for requestId={}: {}", requestId, ex.getMessage());
      }
    }
  }

  @Override
  public void completeWithError(String requestId, Throwable throwable) {
    SseEmitter emitter = emitters.remove(requestId);
    if (emitter != null) {
      try {
        emitter.completeWithError(Objects.requireNonNull(throwable));
        log.debug("SSE emitter completed with error: requestId={}", requestId);
      } catch (Exception ex) {
        log.warn(
          "Error completing SSE emitter with error for requestId={}: {}",
          requestId, ex.getMessage()
        );
      }
    }
  }
}
