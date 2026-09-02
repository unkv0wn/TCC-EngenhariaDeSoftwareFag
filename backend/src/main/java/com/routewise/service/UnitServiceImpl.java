package com.routewise.service;

import com.routewise.dto.UnitDto;
import com.routewise.dto.UnitRequestDto;
import com.routewise.entity.Unit;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UnitServiceImpl implements IUnitService {

  private final UnitRepository repository;

  public UnitServiceImpl(UnitRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UnitDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public UnitDto create(UnitRequestDto request) {
    Unit entity = new Unit(UUID.randomUUID().toString(), request.code().toUpperCase(), request.name());
    return toDto(repository.save(entity));
  }

  @Override
  public UnitDto update(String id, UnitRequestDto request) {
    Unit entity = findOrThrow(id);
    entity.setCode(request.code().toUpperCase());
    entity.setName(request.name());
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(String id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Unidade não encontrada: " + id);
    }
    repository.deleteById(id);
  }

  private Unit findOrThrow(String id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + id));
  }

  private UnitDto toDto(Unit entity) {
    return new UnitDto(entity.getId(), entity.getCode(), entity.getName());
  }
}
