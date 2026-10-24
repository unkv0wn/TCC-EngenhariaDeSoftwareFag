package com.routewise.dto;

/**
 * Resposta da criação de um cliente — a única vez que o segredo existe fora do hash.
 *
 * <p>Tipo próprio, e não um campo opcional no {@link ApiClientDto}, de propósito: um DTO
 * que às vezes carrega segredo e às vezes não convida a ser devolvido por engano numa
 * listagem. Aqui o tipo diz que isto só sai de um lugar.
 */
public record ApiClientCreatedDto(
  ApiClientDto client,
  /** Em claro, uma única vez. Não é recuperável depois — some do servidor. */
  String clientSecret
) {}
