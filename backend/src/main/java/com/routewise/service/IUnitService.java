package com.routewise.service;

import com.routewise.dto.UnitDto;
import com.routewise.dto.UnitRequestDto;

import java.util.List;
import java.util.UUID;

public interface IUnitService {

  List<UnitDto> findAll();

  UnitDto create(UnitRequestDto request);

  UnitDto update(UUID id, UnitRequestDto request);

  void delete(UUID id);
}
