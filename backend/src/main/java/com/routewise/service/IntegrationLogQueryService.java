package com.routewise.service;

import com.routewise.dto.IntegrationRequestLogDto;
import com.routewise.entity.ApiClient;
import com.routewise.entity.IntegrationRequestLog;
import com.routewise.repository.ApiClientRepository;
import com.routewise.repository.IntegrationRequestLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leitura do registro de acesso para a tela do gestor.
 *
 * <p>Separado do {@link IntegrationAuditService}, que escreve, porque os dois têm
 * exigências opostas de transação: aquele precisa de {@code REQUIRES_NEW} para sobreviver
 * ao rollback de uma recusa; este é leitura simples. Juntá-los faria uma consulta de tela
 * abrir transação nova sem motivo.
 */
@Service
@Transactional(readOnly = true)
public class IntegrationLogQueryService {

  /** Teto do que a tela pede de uma vez, mesmo que alguém peça mais. */
  private static final int MAX_LIMIT = 500;

  /**
   * Pseudo-id para pedir só as chamadas sem client identificado.
   *
   * <p>Um UUID zerado em vez de uma string à parte no contrato: o parâmetro continua
   * sendo um UUID só, e um client real nunca nasce com este id.
   */
  public static final UUID UNIDENTIFIED = new UUID(0L, 0L);

  private final IntegrationRequestLogRepository repository;
  private final ApiClientRepository clientRepository;

  public IntegrationLogQueryService(
    IntegrationRequestLogRepository repository,
    ApiClientRepository clientRepository
  ) {
    this.repository = repository;
    this.clientRepository = clientRepository;
  }

  /**
   * Últimas chamadas, opcionalmente de um client só.
   *
   * <p>O filtro vai ao banco em vez de à tela porque o limite é aplicado <em>antes</em>:
   * filtrar as 100 linhas mais recentes da frota inteira mostraria quase nada de um
   * client que parou de chamar semana passada, e o gestor leria isso como "não houve
   * acesso" em vez de "o recorte não alcança".
   *
   * @param clientId client a filtrar; {@code null} traz todos, {@link #UNIDENTIFIED}
   *                 traz só as recusas que não identificaram client
   */
  public List<IntegrationRequestLogDto> recent(int limit, UUID clientId) {
    int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
    PageRequest page = PageRequest.of(0, safeLimit);

    List<IntegrationRequestLog> entries;
    if (clientId == null) {
      entries = repository.findAllByOrderByOccurredAtDesc(page);
    } else if (UNIDENTIFIED.equals(clientId)) {
      entries = repository.findByApiClientIdIsNullOrderByOccurredAtDesc(page);
    } else {
      entries = repository.findByApiClientIdOrderByOccurredAtDesc(clientId, page);
    }

    Map<UUID, String> names = clientRepository.findAll().stream()
      .collect(Collectors.toMap(ApiClient::getId, ApiClient::getName));

    return entries.stream()
      .map(entry -> new IntegrationRequestLogDto(
        entry.getId(),
        entry.getApiClientId(),
        entry.getApiClientId() == null ? null : names.get(entry.getApiClientId()),
        entry.getAttemptedClientId(),
        entry.getMethod(),
        entry.getPath(),
        entry.getOutcome(),
        entry.getStatusCode(),
        entry.getOccurredAt()
      ))
      .toList();
  }
}
