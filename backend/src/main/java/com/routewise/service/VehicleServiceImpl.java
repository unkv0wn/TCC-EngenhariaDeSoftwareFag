package com.routewise.service;

import com.routewise.dto.VehicleDto;
import com.routewise.dto.VehicleRequestDto;
import com.routewise.entity.Vehicle;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.RefuelingRepository;
import com.routewise.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class VehicleServiceImpl implements IVehicleService {

  private final VehicleRepository repository;
  private final RefuelingRepository refuelingRepository;

  public VehicleServiceImpl(VehicleRepository repository, RefuelingRepository refuelingRepository) {
    this.repository = repository;
    this.refuelingRepository = refuelingRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleDto> findAll() {
    Map<UUID, Double> lastOdometers = lastOdometerByVehicle();
    return repository.findAll().stream()
      .map(vehicle -> toDto(vehicle, lastOdometers.get(vehicle.getId())))
      .toList();
  }

  @Override
  public VehicleDto create(VehicleRequestDto request) {
    Vehicle entity = new Vehicle(
      UUID.randomUUID(),
      request.plate().toUpperCase(),
      request.model(),
      request.brand(),
      request.year(),
      request.color(),
      request.capacityKg(),
      request.fuelType()
    );
    entity.setInitialOdometerKm(request.initialOdometerKm());
    return toDto(repository.save(entity));
  }

  @Override
  public VehicleDto update(UUID id, VehicleRequestDto request) {
    Vehicle entity = findOrThrow(id);
    entity.setPlate(request.plate().toUpperCase());
    entity.setModel(request.model());
    entity.setBrand(request.brand());
    entity.setYear(request.year());
    entity.setColor(request.color());
    entity.setCapacityKg(request.capacityKg());
    entity.setFuelType(request.fuelType());
    entity.setInitialOdometerKm(request.initialOdometerKm());
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Veículo não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  private Vehicle findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + id));
  }

  /** Maior odômetro já abastecido, por veículo. */
  private Map<UUID, Double> lastOdometerByVehicle() {
    Map<UUID, Double> byVehicle = new HashMap<>();
    for (Object[] row : refuelingRepository.findMaxOdometerByVehicle()) {
      byVehicle.put((UUID) row[0], (Double) row[1]);
    }
    return byVehicle;
  }

  private VehicleDto toDto(Vehicle entity) {
    return toDto(entity, lastOdometerByVehicle().get(entity.getId()));
  }

  /**
   * @param lastRefueledOdometerKm maior leitura abastecida deste veículo, ou null se não
   *                               houver abastecimento
   */
  private VehicleDto toDto(Vehicle entity, Double lastRefueledOdometerKm) {
    return new VehicleDto(
      entity.getId(),
      entity.getPlate(),
      entity.getModel(),
      entity.getBrand(),
      entity.getYear(),
      entity.getColor(),
      entity.getCapacityKg(),
      entity.getFuelType(),
      entity.getInitialOdometerKm(),
      currentOdometer(entity.getInitialOdometerKm(), lastRefueledOdometerKm)
    );
  }

  /**
   * Odômetro atual: a maior das duas leituras conhecidas.
   *
   * <p>O máximo, e não "a do último abastecimento", porque as duas podem discordar — um
   * veículo cadastrado com 80.000 km cujo primeiro abastecimento foi lançado com 79.500
   * por erro de digitação não deve passar a marcar menos do que já marcava. Null quando
   * não há leitura nenhuma, que é diferente de zero.
   */
  private Double currentOdometer(Double initialKm, Double lastRefueledKm) {
    if (initialKm == null) return lastRefueledKm;
    if (lastRefueledKm == null) return initialKm;
    return Math.max(initialKm, lastRefueledKm);
  }
}
