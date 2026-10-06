package com.routewise.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera o token de acesso que o app do motorista usa pra abrir uma rota.
 *
 * <p>32 bytes de {@link SecureRandom} em base64url sem padding — 43 caracteres, seguros
 * em URL e em deep link. Não é autenticação: é uma URL não-adivinhável. Quem tem o token
 * tem aquela rota (e só aquela). Se vazar, o gestor regenera.
 *
 * <p>{@code SecureRandom}, e não {@code Random} nem {@code UUID.randomUUID()}: o primeiro
 * é previsível a partir de saídas anteriores, e o segundo gasta só 122 bits em um formato
 * que convida a confundir token com id de recurso.
 */
@Component
public class RouteAccessTokenGenerator {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom random = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }
}
