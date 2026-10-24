package com.routewise.exception;

/**
 * Credencial válida, escopo que ela não tem (403).
 *
 * <p>Separado do 401 de propósito: o integrador autenticou direito, e a mensagem precisa
 * mandá-lo pedir o escopo ao gestor, não revisar a credencial.
 */
public class IntegrationScopeException extends RuntimeException {

  public IntegrationScopeException(String message) {
    super(message);
  }
}
