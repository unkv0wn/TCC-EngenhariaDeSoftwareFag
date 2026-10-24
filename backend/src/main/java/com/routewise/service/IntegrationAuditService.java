package com.routewise.service;

import com.routewise.entity.IntegrationRequestLog;
import com.routewise.repository.IntegrationRequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Grava o que aconteceu em cada acesso à superfície de integração.
 *
 * <h2>Por que transação própria</h2>
 *
 * <p>{@code REQUIRES_NEW}, e isso é o ponto inteiro desta classe. As recusas — credencial
 * inválida, cliente revogado, escopo insuficiente — são sinalizadas por exceção. Se o
 * registro fosse gravado na mesma transação de quem recusa, o rollback o levaria junto, e
 * a tabela ficaria com exatamente o oposto do que se quer: só os sucessos, nunca as
 * tentativas barradas.
 *
 * <p>Em transação separada o registro faz commit sozinho, antes de a exceção subir.
 *
 * <h2>Por que nunca lança</h2>
 *
 * <p>Falha ao auditar não pode derrubar a requisição auditada. Um erro aqui vira log de
 * aplicação e segue — perder uma linha de auditoria é ruim; recusar um acesso legítimo
 * porque a auditoria falhou é pior.
 */
@Service
public class IntegrationAuditService {

  private static final Logger log = LoggerFactory.getLogger(IntegrationAuditService.class);

  private final IntegrationRequestLogRepository repository;

  public IntegrationAuditService(IntegrationRequestLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
    UUID apiClientId,
    String attemptedClientId,
    String method,
    String path,
    String outcome,
    int statusCode
  ) {
    try {
      repository.save(new IntegrationRequestLog(
        apiClientId,
        attemptedClientId,
        method,
        path,
        outcome,
        statusCode
      ));
    } catch (RuntimeException ex) {
      log.error("Falha ao registrar acesso de integração ({} {} -> {})", method, path, outcome, ex);
    }
  }
}
