package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    "planejada", List.of("em_rota", "concluida"),
    "em_rota", List.of("concluida", "planejada"),
    "concluida", List.of()
  );

  private final SavedRouteRepository repository;
  private final ObjectMapper objectMapper;

  public SavedRouteServiceImpl(SavedRouteRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
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
  public SavedRouteDto create(SavedRouteRequestDto request) {
    SavedRoute entity = new SavedRoute(UUID.randomUUID());
    entity.setDriverId(request.driverId());
    entity.setVehicleId(request.vehicleId());
    entity.setDepartureTime(request.departureTime());
    entity.setStatus("planejada");
    entity.setOrdersCount(request.ordersCount());
    entity.setTotalValue(request.totalValue());
    entity.setCompletedStops(0);
    entity.setResult(writeJson(request.result()));
    entity.setStops(writeJson(request.stops()));

    // Distância/tempo desnormalizados do snapshot, pra o dashboard somar sem abrir o JSON.
    JsonNode result = request.result();
    entity.setTotalDistanceKm(result.path("totalDistanceKm").asDouble());
    entity.setTotalDurationMin(result.path("totalDurationMin").asDouble());

    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Rota não encontrada: " + id);
    }
    repository.deleteById(id);
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
      readJson(entity.getStops())
    );
  }
}
