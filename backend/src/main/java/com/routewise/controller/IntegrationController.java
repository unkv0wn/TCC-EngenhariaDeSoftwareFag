package com.routewise.controller;

import com.routewise.dto.IntegrationDeliveryDto;
import com.routewise.dto.IntegrationOrderDto;
import com.routewise.dto.IntegrationRouteDto;
import com.routewise.service.IntegrationDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * A superfície que sistemas externos consomem.
 *
 * <p>Somente leitura, e separada do CRUD do gestor de propósito — ver
 * {@link com.routewise.service.IntegrationDataService}.
 *
 * <p>Não há checagem de escopo aqui: ela é feita no {@code IntegrationAuthFilter}, que
 * decide pelo caminho. Deixá-la nos métodos faria cada endpoint novo depender de alguém
 * lembrar de anotá-lo, e esquecer significaria um endpoint sem escopo nenhum.
 */
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

  private final IntegrationDataService service;

  public IntegrationController(IntegrationDataService service) {
    this.service = service;
  }

  @GetMapping("/orders")
  public List<IntegrationOrderDto> orders(@RequestParam(required = false) String status) {
    return service.orders(status);
  }

  @GetMapping("/routes")
  public List<IntegrationRouteDto> routes() {
    return service.routes();
  }

  /**
   * Entregas concluídas. O {@code since} existe para o integrador buscar só o que mudou
   * desde a última consulta, em vez de reler tudo a cada poll.
   */
  @GetMapping("/deliveries")
  public List<IntegrationDeliveryDto> deliveries(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
  ) {
    return service.deliveries(since);
  }
}
