package com.routewise.service;

import com.routewise.dto.VehicleDto;
import com.routewise.dto.VehicleRequestDto;
import com.routewise.entity.Vehicle;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VehicleServiceImpl implements IVehicleService {

  private final VehicleRepository repository;

  public VehicleServiceImpl(VehicleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
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

  private VehicleDto toDto(Vehicle entity) {
    return new VehicleDto(
      entity.getId(),
      entity.getPlate(),
      entity.getModel(),
      entity.getBrand(),
      entity.getYear(),
      entity.getColor(),
      entity.getCapacityKg(),
      entity.getFuelType()
    );
  }
}
