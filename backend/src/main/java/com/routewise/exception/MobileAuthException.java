package com.routewise.exception;

/**
 * Usuário ou senha que não conferem, no login do app (401).
 *
 * <p>A mensagem é deliberadamente a mesma para usuário inexistente e senha errada: dizer
 * qual dos dois falhou entrega metade da credencial a quem está tentando adivinhar.
 */
public class MobileAuthException extends RuntimeException {

  public MobileAuthException(String message) {
    super(message);
  }
}
