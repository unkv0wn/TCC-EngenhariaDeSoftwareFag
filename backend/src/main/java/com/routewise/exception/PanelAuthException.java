package com.routewise.exception;

/**
 * E-mail ou senha que não conferem, no login do painel (401).
 *
 * <p>A mensagem é deliberadamente a mesma para e-mail inexistente e senha errada: dizer
 * qual dos dois falhou entrega metade da credencial a quem está tentando adivinhar.
 */
public class PanelAuthException extends RuntimeException {

  public PanelAuthException(String message) {
    super(message);
  }
}
