package com.routewise.controller;

import com.routewise.dto.OrderDto;
import com.routewise.dto.OrderRequestDto;
import com.routewise.dto.OrderStatusRequestDto;
import com.routewise.service.IOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final IOrderService service;

  public OrderController(IOrderService service) {
    this.service = service;
  }

  @GetMapping
  public List<OrderDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderDto create(@Valid @RequestBody OrderRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public OrderDto update(@PathVariable UUID id, @Valid @RequestBody OrderRequestDto request) {
    return service.update(id, request);
  }

  @PatchMapping("/{id}/status")
  public OrderDto changeStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusRequestDto request) {
    return service.changeStatus(id, request.status());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
