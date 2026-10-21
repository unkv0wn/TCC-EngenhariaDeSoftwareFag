package com.routewise.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Acesso do app como o gestor o vê. Sem nada derivado da senha — nem o hash: ele não
 * serve a nenhuma tela e vazaria num log de rede.
 */
public record MobileUserDto(
  UUID id,
  UUID driverId,
  /** Resolvido na leitura pra tela não ter que cruzar com a lista de motoristas. */
  String driverName,
  String username,
  String deviceId,
  String deviceLabel,
  String status,
  Instant lastLoginAt
) {}
