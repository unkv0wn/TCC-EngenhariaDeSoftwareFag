package com.routewise.service;

import com.routewise.dto.PaymentConditionDto;
import com.routewise.dto.PaymentConditionRequestDto;

import java.util.List;
import java.util.UUID;

public interface IPaymentConditionService {

  List<PaymentConditionDto> findAll();

  PaymentConditionDto create(PaymentConditionRequestDto request);

  PaymentConditionDto update(UUID id, PaymentConditionRequestDto request);

  void delete(UUID id);
}
