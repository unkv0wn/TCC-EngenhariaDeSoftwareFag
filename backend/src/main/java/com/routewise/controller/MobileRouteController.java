package com.routewise.controller;

import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.RouteEventDto;
import com.routewise.service.IMobileRouteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Superfície do app do motorista, separada do CRUD do gestor de propósito.
 *
 * <p>O app enxerga uma rota só, pela {@code token}, e só pode fazer o que um motorista faz:
 * ler a rota e registrar evento. Reaproveitar {@code /api/routes/{id}} entregaria ao celular
 * o id interno e, com ele, o caminho pro resto da API — que hoje está aberta.
 *
 * <p>O {@code batch} existe pela fila offline: entrega acontece em galpão e subsolo, e os
 * eventos sobem em bloco quando a rede volta.
 */
@RestController
@RequestMapping("/api/mobile/routes/{token}")
@Validated
public class MobileRouteController {

  private final IMobileRouteService service;

  public MobileRouteController(IMobileRouteService service) {
    this.service = service;
  }

  @GetMapping
  public MobileRouteDto findByToken(@PathVariable String token) {
    return service.findByToken(token);
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  public RouteEventDto createEvent(@PathVariable String token, @Valid @RequestBody MobileEventRequestDto request) {
    return service.createEvent(token, request);
  }

  @PostMapping("/events/batch")
  public List<MobileEventBatchResultDto> createEvents(
    @PathVariable String token,
    @RequestBody @NotEmpty(message = "Envie ao menos um evento.") List<@Valid MobileEventRequestDto> requests
  ) {
    return service.createEvents(token, requests);
  }
}
