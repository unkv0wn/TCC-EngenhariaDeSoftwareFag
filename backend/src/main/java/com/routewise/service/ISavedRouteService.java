package com.routewise.service;

import com.routewise.dto.SavedRouteDto;
import com.routewise.dto.SavedRouteRequestDto;

import java.util.List;
import java.util.UUID;

public interface ISavedRouteService {

  List<SavedRouteDto> findAll();

  SavedRouteDto findById(UUID id);

  SavedRouteDto create(SavedRouteRequestDto request);

  void delete(UUID id);

  /** Muda só o status, validando a máquina de estados (ver VALID_STATUS_TRANSITIONS). */
  SavedRouteDto changeStatus(UUID id, String status);
}
