package com.routewise.dto;

import java.util.UUID;

/**
 * Resultado de um item do lote de sincronização.
 *
 * <p>Item a item de propósito: um evento rejeitado não pode derrubar os outros, senão o
 * motorista perde registros que fez em campo. O app casa a resposta pelo
 * {@code clientEventId} e só então tira o evento da fila local.
 *
 * @param accepted true quando o evento está gravado — inclusive se já estava, por reenvio
 * @param error    motivo da recusa, nulo quando aceito
 */
public record MobileEventBatchResultDto(
  UUID clientEventId,
  boolean accepted,
  RouteEventDto event,
  String error
) {

  public static MobileEventBatchResultDto accepted(UUID clientEventId, RouteEventDto event) {
    return new MobileEventBatchResultDto(clientEventId, true, event, null);
  }

  public static MobileEventBatchResultDto rejected(UUID clientEventId, String error) {
    return new MobileEventBatchResultDto(clientEventId, false, null, error);
  }
}
