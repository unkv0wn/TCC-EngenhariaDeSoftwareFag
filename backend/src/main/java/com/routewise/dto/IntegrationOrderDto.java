package com.routewise.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pedido como um sistema externo o enxerga.
 *
 * <p>Forma própria, e não o {@link OrderDto} do painel: aquele tem o formato das telas e
 * muda quando elas mudam. Este é um contrato prometido a terceiros — se ele acompanhasse
 * o painel, toda refatoração interna quebraria a integração de alguém.
 *
 * <p>Por isso também não traz ids internos de veículo, motorista ou forma de pagamento:
 * fora daqui esses UUIDs não significam nada, e entregá-los convidaria o integrador a
 * montar chamadas ao CRUD do gestor.
 */
public record IntegrationOrderDto(
  UUID id,
  LocalDate date,
  String status,
  String customerName,
  double total,
  /** Nulo até a entrega acontecer. Horário real do registro em campo. */
  Instant deliveredAt
) {}
