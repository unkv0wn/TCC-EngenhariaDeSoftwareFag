package com.routewise.controller;

import com.routewise.auth.MobileAuthFilter;
import com.routewise.auth.MobileSession;
import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.MobileRouteSummaryDto;
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
 * <p>O app enxerga as rotas dele e só pode fazer o que um motorista faz: ler a rota e
 * registrar evento. Reaproveitar {@code /api/routes/{id}} entregaria ao celular o id
 * interno e, com ele, o caminho pro resto da API — que hoje está aberta.
 *
 * <p>Quem é o motorista vem da sessão, nunca do caminho ou do corpo: o
 * {@link MobileAuthFilter} valida o token e deixa a {@link MobileSession} no request.
 * Aceitar um {@code driverId} vindo do cliente seria deixar o aparelho escolher em nome
 * de quem está falando.
 *
 * <p>O {@code batch} existe pela fila offline: entrega acontece em galpão e subsolo, e os
 * eventos sobem em bloco quando a rede volta.
 */
@RestController
@RequestMapping("/api/mobile/routes")
@Validated
public class MobileRouteController {

  private final IMobileRouteService service;

  public MobileRouteController(IMobileRouteService service) {
    this.service = service;
  }

  /** Tela inicial do app: as rotas em aberto deste motorista e o histórico recente. */
  @GetMapping
  public List<MobileRouteSummaryDto> findMine(
    @RequestAttribute(MobileAuthFilter.SESSION_ATTRIBUTE) MobileSession session
  ) {
    return service.findByDriver(session.driverId());
  }

  @GetMapping("/{code}")
  public MobileRouteDto findByCode(
    @PathVariable String code,
    @RequestAttribute(MobileAuthFilter.SESSION_ATTRIBUTE) MobileSession session
  ) {
    return service.findByCode(code, session.driverId());
  }

  /**
   * Encerra a rota. Pedido separado do evento de retorno de propósito: o evento é um
   * registro histórico que sobe pela fila offline, com o horário em que aconteceu, e o
   * encerramento é uma mudança de estado que só faz sentido com o servidor na mão.
   */
  @PostMapping("/{code}/finish")
  public MobileRouteDto finish(
    @PathVariable String code,
    @RequestAttribute(MobileAuthFilter.SESSION_ATTRIBUTE) MobileSession session
  ) {
    return service.finish(code, session.driverId());
  }

  @PostMapping("/{code}/events")
  @ResponseStatus(HttpStatus.CREATED)
  public RouteEventDto createEvent(
    @PathVariable String code,
    @RequestAttribute(MobileAuthFilter.SESSION_ATTRIBUTE) MobileSession session,
    @Valid @RequestBody MobileEventRequestDto request
  ) {
    return service.createEvent(code, session.driverId(), request);
  }

  @PostMapping("/{code}/events/batch")
  public List<MobileEventBatchResultDto> createEvents(
    @PathVariable String code,
    @RequestAttribute(MobileAuthFilter.SESSION_ATTRIBUTE) MobileSession session,
    @RequestBody @NotEmpty(message = "Envie ao menos um evento.") List<@Valid MobileEventRequestDto> requests
  ) {
    return service.createEvents(code, session.driverId(), requests);
  }
}
