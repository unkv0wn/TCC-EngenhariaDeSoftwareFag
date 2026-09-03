package com.routewise.service;

import com.routewise.dto.VehicleDto;
import com.routewise.dto.VehicleRequestDto;

import java.util.List;
import java.util.UUID;

public interface IVehicleService {

  List<VehicleDto> findAll();

  VehicleDto create(VehicleRequestDto request);

  VehicleDto update(UUID id, VehicleRequestDto request);

  void delete(UUID id);
}
