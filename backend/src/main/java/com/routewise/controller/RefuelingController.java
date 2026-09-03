package com.routewise.controller;

import com.routewise.dto.RefuelingDto;
import com.routewise.dto.RefuelingRequestDto;
import com.routewise.service.IRefuelingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/refuelings")
public class RefuelingController {

  private final IRefuelingService service;

  public RefuelingController(IRefuelingService service) {
    this.service = service;
  }

  @GetMapping
  public List<RefuelingDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RefuelingDto create(@Valid @RequestBody RefuelingRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public RefuelingDto update(@PathVariable UUID id, @Valid @RequestBody RefuelingRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
