package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Corpo pra registrar um evento de rota.
 *
 * <p>A rota vem do path, não do corpo. {@code type} e {@code authorRole} são validados
 * contra listas fechadas no serviço — a mensagem de erro precisa listar os valores
 * aceitos, o que uma anotação de Bean Validation não faria bem.
 *
 * <p>{@code occurredAt} é opcional: quando não vem, o serviço usa o instante atual —
 * o caso comum de registrar algo no momento em que acontece.
 */
public record RouteEventRequestDto(

  @PositiveOrZero(message = "Índice de parada inválido.")
  Integer stopIndex,

  @NotBlank(message = "Informe o tipo do evento.")
  String type,

  @NotBlank(message = "Informe quem está registrando.")
  String authorRole,

  @NotBlank(message = "Informe o nome de quem está registrando.")
  @Size(max = 120, message = "Nome muito longo.")
  String authorName,

  String notes,

  Instant occurredAt

) {}
