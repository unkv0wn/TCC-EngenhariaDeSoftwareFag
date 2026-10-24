package com.routewise.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma entrega concluída.
 *
 * <p>É o endpoint que fecha o ciclo para quem integra: o ERP empurra o pedido, consulta
 * aqui e sabe que a mercadoria chegou, quando e por quem — sem precisar varrer pedidos
 * procurando mudança de status.
 */
public record IntegrationDeliveryDto(
  UUID orderId,
  String customerName,
  Instant deliveredAt,
  double total
) {}
