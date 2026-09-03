package com.routewise.controller;

import com.routewise.dto.PaymentConditionDto;
import com.routewise.dto.PaymentConditionRequestDto;
import com.routewise.service.IPaymentConditionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-conditions")
public class PaymentConditionController {

  private final IPaymentConditionService service;

  public PaymentConditionController(IPaymentConditionService service) {
    this.service = service;
  }

  @GetMapping
  public List<PaymentConditionDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentConditionDto create(@Valid @RequestBody PaymentConditionRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public PaymentConditionDto update(@PathVariable UUID id, @Valid @RequestBody PaymentConditionRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
