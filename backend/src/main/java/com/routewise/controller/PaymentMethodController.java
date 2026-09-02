package com.routewise.controller;

import com.routewise.dto.PaymentMethodDto;
import com.routewise.dto.PaymentMethodRequestDto;
import com.routewise.service.IPaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

  private final IPaymentMethodService service;

  public PaymentMethodController(IPaymentMethodService service) {
    this.service = service;
  }

  @GetMapping
  public List<PaymentMethodDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentMethodDto create(@Valid @RequestBody PaymentMethodRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public PaymentMethodDto update(@PathVariable String id, @Valid @RequestBody PaymentMethodRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
