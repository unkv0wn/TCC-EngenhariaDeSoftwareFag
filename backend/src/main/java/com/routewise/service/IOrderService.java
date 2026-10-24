package com.routewise.service;

import com.routewise.dto.OrderDto;
import com.routewise.dto.OrderRequestDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IOrderService {

  List<OrderDto> findAll();

  OrderDto create(OrderRequestDto request);

  OrderDto update(UUID id, OrderRequestDto request);

  void delete(UUID id);

  /** Muda só o status, validando a máquina de estados (ver VALID_STATUS_TRANSITIONS). */
  OrderDto changeStatus(UUID id, String status);

  /**
   * Como acima, informando quando o fato aconteceu.
   *
   * <p>Só importa para a entrega: o {@code occurredAt} do evento do motorista vira o
   * {@code deliveredAt} do pedido. Uma entrega registrada sem sinal sobe pela fila horas
   * depois, e usar o instante da sincronização marcaria a entrega na hora errada.
   */
  OrderDto changeStatus(UUID id, String status, Instant occurredAt);
}
