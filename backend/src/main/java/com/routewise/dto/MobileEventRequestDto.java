package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento vindo do app do motorista.
 *
 * <p>Não tem {@code authorRole} nem {@code authorName}: o papel é sempre {@code motorista}
 * e o nome é o do motorista escalado na rota — os dois são preenchidos no servidor. Deixar
 * o app escolher permitiria que um evento registrado em campo se apresentasse como sendo
 * do gestor.
 *
 * <p>{@code clientEventId} é obrigatório aqui (ao contrário da web): todo evento do app
 * passa pela fila offline, e sem ele um reenvio duplicaria a entrega.
 */
public record MobileEventRequestDto(

  @NotNull(message = "Informe o identificador do evento no aparelho.")
  UUID clientEventId,

  @NotBlank(message = "Informe o tipo do evento.")
  String type,

  @PositiveOrZero(message = "Índice de parada inválido.")
  Integer stopIndex,

  String notes,

  /** Quando o fato aconteceu no celular — pode ser bem antes de a rede voltar. */
  Instant occurredAt

) {}
