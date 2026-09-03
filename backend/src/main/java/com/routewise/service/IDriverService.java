package com.routewise.service;

import com.routewise.dto.DriverDto;
import com.routewise.dto.DriverRequestDto;

import java.util.List;
import java.util.UUID;

public interface IDriverService {

  List<DriverDto> findAll();

  DriverDto create(DriverRequestDto request);

  DriverDto update(UUID id, DriverRequestDto request);

  void delete(UUID id);
}
