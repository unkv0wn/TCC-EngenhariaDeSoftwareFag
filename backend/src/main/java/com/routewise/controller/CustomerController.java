package com.routewise.controller;

import com.routewise.dto.CustomerDto;
import com.routewise.dto.CustomerRequestDto;
import com.routewise.service.ICustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

  private final ICustomerService service;

  public CustomerController(ICustomerService service) {
    this.service = service;
  }

  @GetMapping
  public List<CustomerDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerDto create(@Valid @RequestBody CustomerRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public CustomerDto update(@PathVariable UUID id, @Valid @RequestBody CustomerRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
