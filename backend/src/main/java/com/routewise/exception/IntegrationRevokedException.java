package com.routewise.exception;

/**
 * Credencial correta, integração revogada (403).
 *
 * <p>Não é 401: o segredo conferiu. Dizer "não autenticado" mandaria o integrador revisar
 * a credencial, quando o que ele precisa é falar com o gestor. E separar os dois mantém a
 * resposta coerente com o que a auditoria grava — antes a tabela registrava 403 e o
 * cliente recebia 401.
 */
public class IntegrationRevokedException extends RuntimeException {

  public IntegrationRevokedException(String message) {
    super(message);
  }
}
