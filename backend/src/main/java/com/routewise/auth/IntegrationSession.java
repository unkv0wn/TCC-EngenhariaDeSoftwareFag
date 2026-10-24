package com.routewise.auth;

import java.util.Set;

/**
 * Qual sistema está chamando, e o que ele pode ler.
 *
 * <p>Os escopos vêm do token, não do banco: a checagem acontece em toda requisição e
 * reler o cliente a cada uma custaria uma consulta por chamada. O efeito colateral é que
 * mudar escopo só vale do próximo token em diante — aceitável porque o token dura pouco,
 * e é por isso que ele dura pouco.
 */
public record IntegrationSession(String clientId, Set<String> scopes) {

  public boolean hasScope(String scope) {
    return scopes.contains(scope);
  }
}
