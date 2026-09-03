package com.routewise.service;

import com.routewise.dto.RefuelingDto;
import com.routewise.dto.RefuelingRequestDto;
import com.routewise.entity.Refueling;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.RefuelingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RefuelingServiceImpl implements IRefuelingService {

  private final RefuelingRepository repository;

  public RefuelingServiceImpl(RefuelingRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RefuelingDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public RefuelingDto create(RefuelingRequestDto request) {
    Refueling entity = new Refueling(
      UUID.randomUUID(),
      request.vehicleId(),
      request.driverId(),
      request.date(),
      request.odometerKm(),
      request.litersRefueled(),
      request.pricePerLiter()
    );
    return toDto(repository.save(entity));
  }

  @Override
  public RefuelingDto update(UUID id, RefuelingRequestDto request) {
    Refueling entity = findOrThrow(id);
    entity.setVehicleId(request.vehicleId());
    entity.setDriverId(request.driverId());
    entity.setDate(request.date());
    entity.setOdometerKm(request.odometerKm());
    entity.setLitersRefueled(request.litersRefueled());
    entity.setPricePerLiter(request.pricePerLiter());
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Abastecimento não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  private Refueling findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Abastecimento não encontrado: " + id));
  }

  private RefuelingDto toDto(Refueling entity) {
    return new RefuelingDto(
      entity.getId(),
      entity.getVehicleId(),
      entity.getDriverId(),
      entity.getDate(),
      entity.getOdometerKm(),
      entity.getLitersRefueled(),
      entity.getPricePerLiter()
    );
  }
}
