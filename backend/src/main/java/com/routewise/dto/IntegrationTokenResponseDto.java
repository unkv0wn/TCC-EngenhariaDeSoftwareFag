package com.routewise.dto;

import java.util.List;

/**
 * Token de acesso emitido para um integrador.
 *
 * <p>Os nomes dos campos seguem o OAuth 2 (`access_token`, `token_type`, `expires_in`)
 * porque é o que as bibliotecas de cliente esperam encontrar — um integrador não deveria
 * precisar de código especial para falar com este sistema.
 */
public record IntegrationTokenResponseDto(
  String accessToken,
  String tokenType,
  long expiresIn,
  List<String> scopes
) {}
