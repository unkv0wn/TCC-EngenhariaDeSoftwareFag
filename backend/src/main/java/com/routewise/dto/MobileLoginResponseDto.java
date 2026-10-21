package com.routewise.dto;

import java.util.UUID;

/**
 * Sessão do motorista.
 *
 * <p>O nome vem junto só para o app saber quem está logado antes de abrir qualquer rota —
 * é o que a tela de abertura mostra no cabeçalho. Ele <strong>não</strong> é o autor dos
 * eventos: esse continua sendo o motorista escalado na rota, preenchido no servidor (ver
 * {@code MobileEventRequestDto}), justamente para o app não poder escolher.
 */
public record MobileLoginResponseDto(
  String token,
  UUID driverId,
  String driverName,
  String username
) {}
