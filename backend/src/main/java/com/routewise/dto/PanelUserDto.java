package com.routewise.dto;

import java.time.Instant;
import java.util.UUID;

/** Acesso ao painel, como a tela do gestor o enxerga. Sem hash de senha, nunca. */
public record PanelUserDto(
  UUID id,
  String name,
  String email,
  String status,
  Instant lastLoginAt
) {}
