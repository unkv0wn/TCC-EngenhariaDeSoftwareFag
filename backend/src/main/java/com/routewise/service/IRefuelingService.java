package com.routewise.service;

import com.routewise.dto.RefuelingDto;
import com.routewise.dto.RefuelingRequestDto;

import java.util.List;
import java.util.UUID;

public interface IRefuelingService {

  List<RefuelingDto> findAll();

  RefuelingDto create(RefuelingRequestDto request);

  RefuelingDto update(UUID id, RefuelingRequestDto request);

  void delete(UUID id);
}
