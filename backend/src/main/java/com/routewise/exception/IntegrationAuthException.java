package com.routewise.exception;

/**
 * Credencial de integração que não serve (401).
 *
 * <p>Mensagem igual para cliente inexistente e segredo errado: distinguir os dois
 * confirmaria a existência do clientId a quem está tentando adivinhar.
 */
public class IntegrationAuthException extends RuntimeException {

  public IntegrationAuthException(String message) {
    super(message);
  }
}
