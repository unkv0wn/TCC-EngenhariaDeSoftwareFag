package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.auth.RouteAccessCodeGenerator;
import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.MobileRouteSummaryDto;
import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;
import com.routewise.entity.Driver;
import com.routewise.entity.RouteEvent;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.DriverRepository;
import com.routewise.repository.RouteEventRepository;
import com.routewise.repository.SavedRouteRepository;
import com.routewise.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Superfície do app do motorista. Tudo aqui entra por código, nunca por id.
 *
 * <p>O papel do autor é fixado em {@code motorista} e o nome é lido do cadastro do
 * motorista escalado — nenhum dos dois vem do corpo da requisição. Um aparelho em campo
 * não escolhe em nome de quem está falando.
 */
@Service
@Transactional
public class MobileRouteServiceImpl implements IMobileRouteService {

  private static final Logger log = LoggerFactory.getLogger(MobileRouteServiceImpl.class);

  private static final String AUTHOR_ROLE = "motorista";
  private static final String FALLBACK_AUTHOR_NAME = "Motorista";

  /** Quanto tempo uma rota encerrada continua aparecendo na tela inicial do app. */
  private static final Duration HISTORY_WINDOW = Duration.ofDays(7);

  private final SavedRouteRepository routeRepository;
  private final RouteEventRepository routeEventRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final IRouteEventService routeEventService;
  private final ISavedRouteService savedRouteService;
  private final RouteStopResolver stopResolver;
  private final ObjectMapper objectMapper;
  private final RouteAccessCodeGenerator codeGenerator;

  public MobileRouteServiceImpl(
    SavedRouteRepository routeRepository,
    RouteEventRepository routeEventRepository,
    DriverRepository driverRepository,
    VehicleRepository vehicleRepository,
    IRouteEventService routeEventService,
    ISavedRouteService savedRouteService,
    RouteStopResolver stopResolver,
    ObjectMapper objectMapper,
    RouteAccessCodeGenerator codeGenerator
  ) {
    this.routeRepository      = routeRepository;
    this.routeEventRepository = routeEventRepository;
    this.driverRepository  = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.routeEventService = routeEventService;
    this.savedRouteService = savedRouteService;
    this.stopResolver      = stopResolver;
    this.objectMapper      = objectMapper;
    this.codeGenerator     = codeGenerator;
  }

  /**
   * As rotas deste motorista para a tela inicial.
   *
   * <p>Mostra tudo que está em aberto, sempre, mais as encerradas dos últimos dias. O
   * corte existe porque esta lista é a primeira tela do app e abre o dia inteiro: sem
   * ele, em alguns meses a rota de hoje estaria enterrada sob o histórico, e o motorista
   * rolaria a tela para começar a trabalhar. O histórico curto cobre o caso real de
   * reabrir a rota de ontem para conferir o que foi registrado.
   */
  @Override
  @Transactional(readOnly = true)
  public List<MobileRouteSummaryDto> findByDriver(UUID driverId) {
    Instant cutoff = Instant.now().minus(HISTORY_WINDOW);

    List<SavedRoute> routes = routeRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
      // Rota sem código de acesso não é abrível pelo app: toda esta superfície é
      // endereçada por código. São rotas anteriores à V16, que criou o campo. Listá-las
      // daria ao motorista um cartão que falha ao ser tocado.
      .filter(route -> route.getAccessCode() != null && !route.getAccessCode().isBlank())
      .filter(route -> !RouteStatus.CONCLUIDA.equals(route.getStatus()) || route.getCreatedAt().isAfter(cutoff))
      .toList();

    if (routes.isEmpty()) {
      return List.of();
    }

    // Progresso de todas as rotas numa consulta só. Uma por rota seria N+1 numa tela que
    // o motorista abre a cada parada.
    Map<UUID, Set<Integer>> deliveredByRoute = routeEventRepository
      .findByRouteIdIn(routes.stream().map(SavedRoute::getId).toList())
      .stream()
      .filter(event -> "entrega".equals(event.getType()) && event.getStopIndex() != null)
      .collect(Collectors.groupingBy(
        RouteEvent::getRouteId,
        Collectors.mapping(RouteEvent::getStopIndex, Collectors.toSet())
      ));

    return routes.stream()
      .map(route -> toSummary(route, deliveredByRoute.getOrDefault(route.getId(), Set.of())))
      .toList();
  }

  private MobileRouteSummaryDto toSummary(SavedRoute route, Set<Integer> deliveredStops) {
    Optional<Vehicle> vehicle = vehicleRepository.findById(route.getVehicleId());

    return new MobileRouteSummaryDto(
      route.getAccessCode(),
      route.getCreatedAt(),
      route.getStatus(),
      route.getDepartureTime(),
      route.getOrdersCount(),
      stopResolver.countStops(route),
      deliveredStops.size(),
      route.getTotalDistanceKm(),
      route.getTotalDurationMin(),
      vehicle.map(Vehicle::getPlate).orElse(null),
      vehicle.map(Vehicle::getModel).orElse(null)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public MobileRouteDto findByCode(String accessCode, UUID driverId) {
    return toDto(requireRoute(accessCode, driverId));
  }

  /**
   * Encerra a rota: é o motorista dizendo que voltou ao depósito.
   *
   * <p>Idempotente de propósito. O app chama isto com a rede que tem; se a resposta se
   * perder no caminho e ele repetir, repetir tem que dar certo — senão o motorista fica
   * com uma rota que o servidor já fechou e um botão que insiste em dar erro.
   *
   * <p>O evento de retorno não é criado aqui: ele sobe pela fila, junto com os outros, e
   * carrega o horário em que o retorno de fato aconteceu — que pode ser bem antes deste
   * pedido, se o pátio não tiver sinal.
   */
  @Override
  public MobileRouteDto finish(String accessCode, UUID driverId) {
    SavedRoute route = requireRoute(accessCode, driverId);

    if (!RouteStatus.CONCLUIDA.equals(route.getStatus())) {
      savedRouteService.changeStatus(route.getId(), RouteStatus.CONCLUIDA);
      log.info("Rota encerrada pelo app do motorista: routeId={}", route.getId());
    }
    return toDto(requireRoute(accessCode, driverId));
  }

  private MobileRouteDto toDto(SavedRoute route) {
    Optional<Driver> driver = driverRepository.findById(route.getDriverId());
    Optional<Vehicle> vehicle = vehicleRepository.findById(route.getVehicleId());
    List<RouteEventDto> events = routeEventService.findByRoute(route.getId());

    return new MobileRouteDto(
      route.getAccessCode(),
      route.getCreatedAt(),
      route.getStatus(),
      route.getDepartureTime(),
      route.getOrdersCount(),
      route.getTotalValue(),
      route.getTotalDistanceKm(),
      route.getTotalDurationMin(),
      driver.map(Driver::getFullName).orElse(FALLBACK_AUTHOR_NAME),
      vehicle.map(Vehicle::getPlate).orElse(null),
      vehicle.map(Vehicle::getModel).orElse(null),
      vehicle.map(Vehicle::getCapacityKg).orElse(0.0),
      stopResolver.resolve(route, events),
      readJson(route.getResult()),
      events
    );
  }

  @Override
  public RouteEventDto createEvent(String accessCode, UUID driverId, MobileEventRequestDto request) {
    SavedRoute route = requireRoute(accessCode, driverId);
    return routeEventService.create(route.getId(), toServiceRequest(route, request));
  }

  /**
   * Sem transação própria, de propósito — e isso é o que faz o lote funcionar.
   *
   * <p>Com {@code @Transactional} aqui, cada {@code create()} entraria nesta mesma
   * transação; o primeiro item inválido a lançar marcaria tudo como rollback-only, e o
   * commit no fim estouraria {@code UnexpectedRollbackException} mesmo com a exceção
   * tratada — derrubando também os eventos válidos. Era exatamente o que o retorno item
   * a item existia pra evitar.
   *
   * <p>Fora de transação, cada {@code create()} abre a sua: um item falha sozinho.
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<MobileEventBatchResultDto> createEvents(
    String accessCode,
    UUID driverId,
    List<MobileEventRequestDto> requests
  ) {
    SavedRoute route = requireRoute(accessCode, driverId);

    List<MobileEventBatchResultDto> results = new ArrayList<>(requests.size());
    for (MobileEventRequestDto request : requests) {
      // Um item inválido não pode levar os outros junto: o motorista perderia registros
      // que já fez em campo e não tem como refazer.
      try {
        RouteEventDto event = routeEventService.create(route.getId(), toServiceRequest(route, request));
        results.add(MobileEventBatchResultDto.accepted(request.clientEventId(), event));
      } catch (IllegalArgumentException ex) {
        log.warn("Evento recusado no lote: clientEventId={}, motivo={}", request.clientEventId(), ex.getMessage());
        results.add(MobileEventBatchResultDto.rejected(request.clientEventId(), ex.getMessage()));
      }
    }
    return results;
  }

  private RouteEventRequestDto toServiceRequest(SavedRoute route, MobileEventRequestDto request) {
    String driverName = driverRepository.findById(route.getDriverId())
      .map(Driver::getFullName)
      .orElse(FALLBACK_AUTHOR_NAME);

    return new RouteEventRequestDto(
      request.stopIndex(),
      request.type(),
      AUTHOR_ROLE,
      driverName,
      request.notes(),
      request.occurredAt(),
      request.clientEventId()
    );
  }

  /**
   * A rota pelo código, desde que seja deste motorista.
   *
   * <p>Código desconhecido devolve 404, nunca 403 — um 403 confirmaria que aquele código
   * existe, o que transforma a URL não-adivinhável num alvo de tentativa.
   *
   * <p><strong>Rota de outro motorista também devolve 404</strong>, pela mesma razão e
   * não por descuido: dizer "existe, mas não é sua" entregaria a um app qualquer um
   * oráculo para descobrir códigos válidos. Do lado de fora, a rota alheia é
   * indistinguível de uma que não existe.
   *
   * <p>O preço disto é real: o motorista não cobre mais a rota de um colega que faltou
   * só com o código na mão — o gestor precisa reatribuir a rota a ele antes.
   */
  private SavedRoute requireRoute(String accessCode, UUID driverId) {
    // Normaliza antes de consultar: o código chega de um QR Code (exato), mas também de
    // alguém digitando o que ouviu por telefone — com hífen, em minúscula, com O no lugar
    // de 0. Recusar por isso seria inventar um erro que não existe.
    String normalized = codeGenerator.normalize(accessCode);
    if (normalized == null) {
      throw new ResourceNotFoundException("Rota não encontrada.");
    }

    SavedRoute route = routeRepository.findByAccessCode(normalized)
      .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada."));

    if (!route.getDriverId().equals(driverId)) {
      log.warn("Rota pedida por motorista diferente do escalado: routeId={}", route.getId());
      throw new ResourceNotFoundException("Rota não encontrada.");
    }
    return route;
  }

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON da rota corrompido no banco: " + ex.getOriginalMessage(), ex);
    }
  }
}
