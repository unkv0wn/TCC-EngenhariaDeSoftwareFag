package com.routewise.controller;

import com.routewise.dto.RouteStatusRequestDto;
import com.routewise.dto.SavedRouteDto;
import com.routewise.dto.SavedRouteRequestDto;
import com.routewise.service.ISavedRouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD das rotas salvas. Compartilha o path base com {@link RouteController} (compute/SSE),
 * mas sem conflito de rota: aqui são {@code GET/POST /api/routes} e {@code /api/routes/{id}}.
 */
@RestController
@RequestMapping("/api/routes")
public class SavedRouteController {

  private final ISavedRouteService service;

  public SavedRouteController(ISavedRouteService service) {
    this.service = service;
  }

  @GetMapping
  public List<SavedRouteDto> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public SavedRouteDto findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SavedRouteDto create(@Valid @RequestBody SavedRouteRequestDto request) {
    return service.create(request);
  }

  @PatchMapping("/{id}/status")
  public SavedRouteDto changeStatus(@PathVariable UUID id, @Valid @RequestBody RouteStatusRequestDto request) {
    return service.changeStatus(id, request.status());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
