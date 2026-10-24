package com.routewise.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cliente de integração como o gestor o vê.
 *
 * <p>Sem nada derivado do segredo — nem o hash. Ele não serve a tela nenhuma e vazaria
 * num log de rede.
 */
public record ApiClientDto(
  UUID id,
  String name,
  String clientId,
  List<String> scopes,
  String status,
  Instant lastUsedAt,
  Instant createdAt
) {}
