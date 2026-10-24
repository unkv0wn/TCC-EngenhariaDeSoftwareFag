package com.routewise.service;

import com.routewise.dto.IntegrationRequestLogDto;
import com.routewise.entity.ApiClient;
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

  private final IntegrationRequestLogRepository repository;
  private final ApiClientRepository clientRepository;

  public IntegrationLogQueryService(
    IntegrationRequestLogRepository repository,
    ApiClientRepository clientRepository
  ) {
    this.repository = repository;
    this.clientRepository = clientRepository;
  }

  public List<IntegrationRequestLogDto> recent(int limit) {
    int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);

    Map<UUID, String> names = clientRepository.findAll().stream()
      .collect(Collectors.toMap(ApiClient::getId, ApiClient::getName));

    return repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, safeLimit)).stream()
      .map(entry -> new IntegrationRequestLogDto(
        entry.getId(),
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
