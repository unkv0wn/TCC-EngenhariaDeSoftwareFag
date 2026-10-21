package com.routewise.exception;

/**
 * Credencial certa, aparelho errado (403).
 *
 * <p>Status separado do 401 de propósito: o motorista digitou tudo certo, e a mensagem
 * precisa mandá-lo falar com o gestor em vez de tentar a senha de novo.
 */
public class DeviceNotAuthorizedException extends RuntimeException {

  public DeviceNotAuthorizedException(String message) {
    super(message);
  }
}
