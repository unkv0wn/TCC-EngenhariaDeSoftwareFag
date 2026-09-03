package com.routewise.service;

import com.routewise.dto.OrderDto;
import com.routewise.dto.OrderRequestDto;

import java.util.List;
import java.util.UUID;

public interface IOrderService {

  List<OrderDto> findAll();

  OrderDto create(OrderRequestDto request);

  OrderDto update(UUID id, OrderRequestDto request);

  void delete(UUID id);

  /** Muda só o status, validando a máquina de estados (ver VALID_STATUS_TRANSITIONS). */
  OrderDto changeStatus(UUID id, String status);
}
