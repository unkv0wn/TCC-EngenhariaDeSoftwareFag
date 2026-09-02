package com.routewise.controller;

import com.routewise.dto.DriverDto;
import com.routewise.dto.DriverRequestDto;
import com.routewise.service.IDriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

  private final IDriverService service;

  public DriverController(IDriverService service) {
    this.service = service;
  }

  @GetMapping
  public List<DriverDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DriverDto create(@Valid @RequestBody DriverRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public DriverDto update(@PathVariable UUID id, @Valid @RequestBody DriverRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
