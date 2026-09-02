package com.routewise.controller;

import com.routewise.dto.UnitDto;
import com.routewise.dto.UnitRequestDto;
import com.routewise.service.IUnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
public class UnitController {

  private final IUnitService service;

  public UnitController(IUnitService service) {
    this.service = service;
  }

  @GetMapping
  public List<UnitDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UnitDto create(@Valid @RequestBody UnitRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public UnitDto update(@PathVariable String id, @Valid @RequestBody UnitRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
