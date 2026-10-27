package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.auth.RouteAccessCodeGenerator;
import com.routewise.dto.RouteCostDto;
import com.routewise.dto.SavedRouteDto;
import com.routewise.dto.SavedRouteRequestDto;
import com.routewise.entity.SavedRoute;
import com.routewise.exception.InvalidRouteStatusTransitionException;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.SavedRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SavedRouteServiceImpl implements ISavedRouteService {

  /**
   * Transições válidas — espelha o RouteStatus do front (useRoutes.ts).
   * "planejada" nasce no salvamento; "em_rota" quando o caminhão sai; "concluida"
   * no retorno. Permite voltar de em_rota pra planejada (caso o motorista não saia).
   */
  private static final Map<String, List<String>> VALID_STATUS_TRANSITIONS = Map.of(
    RouteStatus.PLANEJADA, List.of(RouteStatus.EM_ROTA, RouteStatus.CONCLUIDA),
    RouteStatus.EM_ROTA, List.of(RouteStatus.CONCLUIDA, RouteStatus.PLANEJADA),
    RouteStatus.CONCLUIDA, List.of()
  );

  private final SavedRouteRepository repository;
  private final ObjectMapper objectMapper;
  private final RouteAccessCodeGenerator codeGenerator;
  private final RouteOrderLifecycle orderLifecycle;
  private final RouteCostingService costingService;

  public SavedRouteServiceImpl(
    SavedRouteRepository repository,
    ObjectMapper objectMapper,
    RouteAccessCodeGenerator codeGenerator,
    RouteOrderLifecycle orderLifecycle,
    RouteCostingService costingService
  ) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.codeGenerator = codeGenerator;
    this.orderLifecycle = orderLifecycle;
    this.costingService = costingService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedRouteDto> findAll() {
    return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SavedRouteDto findById(UUID id) {
    return toDto(findOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public RouteCostDto cost(UUID id) {
    SavedRoute route = findOrThrow(id);
    RouteCostingService.RouteCost cost = costingService.costOf(route);

    double distanceKm = route.getTotalDistanceKm();
    return new RouteCostDto(
      cost.breakdown().fuelLiters(),
      cost.breakdown().fuelCost(),
      cost.breakdown().tireCost(),
      cost.breakdown().driverCost(),
      cost.breakdown().total(),
      // Rota sem distância não tem R$/km — dividir daria infinito, que a tela mostraria
      // como um número.
      distanceKm > 0 ? cost.breakdown().total() / distanceKm : 0.0,
      distanceKm,
      route.getTotalDurationMin(),
      cost.cargoWeightKg(),
      cost.profile().label(),
      cost.profile().capacityKg(),
      cost.consumptionLPer100Km(),
      cost.consumptionMeasured(),
      cost.fuelPricePerLiter(),
      cost.fuelPriceMeasured()
    );
  }

  @Override
  public SavedRouteDto create(SavedRouteRequestDto request) {
    SavedRoute entity = new SavedRoute(UUID.randomUUID());
    entity.setDriverId(request.driverId());
    entity.setVehicleId(request.vehicleId());
    entity.setDepartureTime(request.departureTime());
    entity.setStatus(RouteStatus.PLANEJADA);
    entity.setOrdersCount(request.ordersCount());
    entity.setTotalValue(request.totalValue());
    entity.setCompletedStops(0);
    // Toda rota nasce com token: o gestor compartilha o link na hora que quiser,
    // sem um passo extra de "gerar acesso".
    entity.setAccessCode(codeGenerator.generate());
    entity.setResult(writeJson(request.result()));
    entity.setStops(writeJson(request.stops()));

    // Distância/tempo desnormalizados do snapshot, pra o dashboard somar sem abrir o JSON.
    JsonNode result = request.result();
    entity.setTotalDistanceKm(result.path("totalDistanceKm").asDouble());
    entity.setTotalDurationMin(result.path("totalDurationMin").asDouble());

    SavedRoute saved = repository.save(entity);

    // Os pedidos saem da fila de faturados agora, não quando o caminhão sai: é o que
    // impede o mesmo pedido de entrar numa segunda rota. Ver RouteOrderLifecycle.
    orderLifecycle.onRouteCreated(saved);

    return toDto(saved);
  }

  @Override
  public void delete(UUID id) {
    SavedRoute entity = findOrThrow(id);

    // Antes de apagar: o que estava em rota volta para a fila. Sem isto o pedido ficaria
    // em `em_rota` apontando para uma rota que não existe mais — fora da fila de
    // faturados e sem nenhuma entrega a caminho.
    orderLifecycle.onRouteClosed(entity, "rota excluída");

    repository.delete(entity);
  }

  @Override
  public SavedRouteDto changeStatus(UUID id, String status) {
    SavedRoute entity = findOrThrow(id);
    if (entity.getStatus().equals(status)) {
      return toDto(entity);
    }
    List<String> allowed = VALID_STATUS_TRANSITIONS.getOrDefault(entity.getStatus(), List.of());
    if (!allowed.contains(status)) {
      throw new InvalidRouteStatusTransitionException(
        "Não é possível mudar a rota de '" + entity.getStatus() + "' para '" + status + "'."
      );
    }
    entity.setStatus(status);
    SavedRoute saved = repository.save(entity);

    // Rota encerrada devolve à fila o que não foi entregue. Vale para o gestor fechando
    // na web e para o motorista fechando no app — o `finish` do app passa por aqui.
    if (RouteStatus.CONCLUIDA.equals(status)) {
      orderLifecycle.onRouteClosed(saved, "rota encerrada");
    }

    return toDto(saved);
  }

  @Override
  public SavedRouteDto regenerateAccessCode(UUID id) {
    SavedRoute entity = findOrThrow(id);
    entity.setAccessCode(codeGenerator.generate());
    return toDto(repository.save(entity));
  }

  private SavedRoute findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada: " + id));
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("JSON da rota inválido: " + ex.getOriginalMessage(), ex);
    }
  }

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON da rota corrompido no banco: " + ex.getOriginalMessage(), ex);
    }
  }

  private SavedRouteDto toDto(SavedRoute entity) {
    return new SavedRouteDto(
      entity.getId(),
      entity.getCreatedAt(),
      entity.getStatus(),
      entity.getCompletedStops(),
      entity.getOrdersCount(),
      entity.getTotalValue(),
      entity.getTotalDistanceKm(),
      entity.getTotalDurationMin(),
      entity.getDriverId(),
      entity.getVehicleId(),
      entity.getDepartureTime(),
      readJson(entity.getResult()),
      readJson(entity.getStops()),
      entity.getAccessCode()
    );
  }
}
