package com.routewise.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha do registro de acesso, como a tela do gestor a mostra.
 *
 * <p>{@code clientName} é resolvido na leitura e fica nulo quando a tentativa não tem
 * dono — client_id que não existe, ou integração já apagada. Nesses casos o que
 * identifica quem tentou é o {@code attemptedClientId}.
 */
public record IntegrationRequestLogDto(
  UUID id,
  /** Null quando a chamada não chegou a identificar um client — ver attemptedClientId. */
  UUID apiClientId,
  String clientName,
  String attemptedClientId,
  String method,
  String path,
  String outcome,
  int statusCode,
  Instant occurredAt
) {}
