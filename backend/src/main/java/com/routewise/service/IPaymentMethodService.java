package com.routewise.service;

import com.routewise.dto.PaymentMethodDto;
import com.routewise.dto.PaymentMethodRequestDto;

import java.util.List;
import java.util.UUID;

public interface IPaymentMethodService {

  List<PaymentMethodDto> findAll();

  PaymentMethodDto create(PaymentMethodRequestDto request);

  PaymentMethodDto update(UUID id, PaymentMethodRequestDto request);

  void delete(UUID id);
}
