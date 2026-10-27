package com.routewise.exception;

/**
 * Credencial correta, acesso suspenso pelo gestor (403).
 *
 * <p>Separado do 401 de propósito: aqui a pessoa provou quem é, e mandá-la tentar a senha
 * de novo seria desperdiçar o tempo dela. 403 com a frase certa diz o que fazer — falar
 * com quem administra o painel.
 */
public class PanelAccessBlockedException extends RuntimeException {

  public PanelAccessBlockedException(String message) {
    super(message);
  }
}
