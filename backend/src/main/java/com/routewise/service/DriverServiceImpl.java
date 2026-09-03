package com.routewise.service;

import com.routewise.dto.DriverDto;
import com.routewise.dto.DriverRequestDto;
import com.routewise.entity.Driver;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DriverServiceImpl implements IDriverService {

  private final DriverRepository repository;

  public DriverServiceImpl(DriverRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public DriverDto create(DriverRequestDto request) {
    Driver entity = new Driver(
      UUID.randomUUID(),
      request.fullName(),
      request.cpf(),
      request.phone(),
      request.cnhNumber(),
      request.cnhCategory(),
      request.cnhValidity(),
      request.status()
    );
    return toDto(repository.save(entity));
  }

  @Override
  public DriverDto update(UUID id, DriverRequestDto request) {
    Driver entity = findOrThrow(id);
    entity.setFullName(request.fullName());
    entity.setCpf(request.cpf());
    entity.setPhone(request.phone());
    entity.setCnhNumber(request.cnhNumber());
    entity.setCnhCategory(request.cnhCategory());
    entity.setCnhValidity(request.cnhValidity());
    entity.setStatus(request.status());
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Motorista não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  private Driver findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado: " + id));
  }

  private DriverDto toDto(Driver entity) {
    return new DriverDto(
      entity.getId(),
      entity.getFullName(),
      entity.getCpf(),
      entity.getPhone(),
      entity.getCnhNumber(),
      entity.getCnhCategory(),
      entity.getCnhValidity(),
      entity.getStatus()
    );
  }
}
