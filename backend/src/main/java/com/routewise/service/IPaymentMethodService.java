package com.routewise.service;

import com.routewise.dto.PaymentMethodDto;
import com.routewise.dto.PaymentMethodRequestDto;

import java.util.List;

public interface IPaymentMethodService {

  List<PaymentMethodDto> findAll();

  PaymentMethodDto create(PaymentMethodRequestDto request);

  PaymentMethodDto update(String id, PaymentMethodRequestDto request);

  void delete(String id);
}
