package com.routewise.controller;

import com.routewise.dto.VehicleDto;
import com.routewise.dto.VehicleRequestDto;
import com.routewise.service.IVehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

  private final IVehicleService service;

  public VehicleController(IVehicleService service) {
    this.service = service;
  }

  @GetMapping
  public List<VehicleDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VehicleDto create(@Valid @RequestBody VehicleRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public VehicleDto update(@PathVariable UUID id, @Valid @RequestBody VehicleRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
