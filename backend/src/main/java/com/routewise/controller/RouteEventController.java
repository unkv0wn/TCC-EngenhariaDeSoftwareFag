package com.routewise.controller;

import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;
import com.routewise.service.IRouteEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Log de eventos de uma rota.
 *
 * <p>Terceiro controller sob {@code /api/routes}, sem colisão com os outros dois:
 * {@link RouteController} usa {@code /events/{requestId}} (SSE, literal no primeiro
 * segmento) e aqui é {@code /{routeId}/events} (literal no segundo). Inverter essa
 * ordem em algum dos dois quebra o roteamento sem erro de compilação — vale conferir
 * antes de mexer.
 *
 * <p>Não há {@code PUT}: um log se corrige apagando e registrando de novo.
 */
@RestController
@RequestMapping("/api/routes/{routeId}/events")
public class RouteEventController {

  private final IRouteEventService service;

  public RouteEventController(IRouteEventService service) {
    this.service = service;
  }

  @GetMapping
  public List<RouteEventDto> findByRoute(@PathVariable UUID routeId) {
    return service.findByRoute(routeId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RouteEventDto create(@PathVariable UUID routeId, @Valid @RequestBody RouteEventRequestDto request) {
    return service.create(routeId, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID routeId, @PathVariable UUID id) {
    service.delete(routeId, id);
  }
}
