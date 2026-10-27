package com.routewise.controller;

import com.routewise.dto.ApiClientCreatedDto;
import com.routewise.dto.ApiClientDto;
import com.routewise.dto.ApiClientRequestDto;
import com.routewise.dto.IntegrationRequestLogDto;
import com.routewise.service.IApiClientService;
import com.routewise.service.IntegrationLogQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Integrações, do lado do gestor.
 *
 * <p>Fica fora de {@code /api/integration/**} de propósito, apesar do assunto: aquele
 * prefixo é a superfície que terceiros consomem e passa pelo filtro de token. Isto é
 * cadastro do painel, e mora com o resto do CRUD.
 */
@RestController
@RequestMapping("/api/api-clients")
public class ApiClientController {

  private final IApiClientService service;
  private final IntegrationLogQueryService logs;

  public ApiClientController(IApiClientService service, IntegrationLogQueryService logs) {
    this.service = service;
    this.logs = logs;
  }

  @GetMapping
  public List<ApiClientDto> findAll() {
    return service.findAll();
  }

  /** Devolve o segredo em claro — a única vez que ele existe fora do hash. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiClientCreatedDto create(@Valid @RequestBody ApiClientRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public ApiClientDto update(@PathVariable UUID id, @Valid @RequestBody ApiClientRequestDto request) {
    return service.update(id, request);
  }

  /** Gera um segredo novo e invalida o anterior. */
  @PostMapping("/{id}/secret")
  public ApiClientCreatedDto regenerateSecret(@PathVariable UUID id) {
    return service.regenerateSecret(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  /**
   * Registro de acesso, do mais recente para o mais antigo.
   *
   * <p>Paginado por obrigação: é a única tabela do projeto que cresce por requisição.
   */
  @GetMapping("/logs")
  public List<IntegrationRequestLogDto> logs(
    @RequestParam(defaultValue = "100") int limit,
    @RequestParam(required = false) UUID clientId
  ) {
    return logs.recent(limit, clientId);
  }
}
