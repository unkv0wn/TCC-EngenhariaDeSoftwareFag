package com.routewise.service;

import com.routewise.dto.UnitDto;
import com.routewise.dto.UnitRequestDto;

import java.util.List;

public interface IUnitService {

  List<UnitDto> findAll();

  UnitDto create(UnitRequestDto request);

  UnitDto update(String id, UnitRequestDto request);

  void delete(String id);
}
