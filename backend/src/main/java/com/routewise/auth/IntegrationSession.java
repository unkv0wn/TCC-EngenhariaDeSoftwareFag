package com.routewise.auth;

import java.util.Set;
import java.util.UUID;

/**
 * Qual sistema está chamando, e o que ele pode ler.
 *
 * <p>Os escopos vêm do token, não do banco: a checagem acontece em toda requisição e
 * reler o cliente a cada uma custaria uma consulta por chamada. O efeito colateral é que
 * mudar escopo só vale do próximo token em diante — aceitável porque o token dura pouco,
 * e é por isso que ele dura pouco.
 *
 * @param clientId    id público da integração, o que viaja na credencial
 * @param apiClientId id interno, para o registro de acesso poder ser filtrado por
 *                    integração; {@code null} em token emitido antes deste campo existir
 * @param scopes      escopos lidos de dentro do token
 */
public record IntegrationSession(String clientId, UUID apiClientId, Set<String> scopes) {

  public boolean hasScope(String scope) {
    return scopes.contains(scope);
  }
}
