package com.routewise.service;

import com.routewise.auth.JwtService;
import com.routewise.dto.ApiClientCreatedDto;
import com.routewise.dto.ApiClientDto;
import com.routewise.dto.ApiClientRequestDto;
import com.routewise.dto.IntegrationTokenRequestDto;
import com.routewise.dto.IntegrationTokenResponseDto;
import com.routewise.entity.ApiClient;
import com.routewise.entity.IntegrationRequestLog;
import com.routewise.exception.IntegrationAuthException;
import com.routewise.exception.IntegrationRevokedException;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.ApiClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApiClientServiceImpl implements IApiClientService {

  /** Mesma frase para cliente inexistente e segredo errado — ver IntegrationAuthException. */
  private static final String BAD_CREDENTIALS = "Credencial de integração inválida.";

  private static final String TOKEN_TYPE = "Bearer";

  /**
   * Prefixo no client_id e no segredo.
   *
   * <p>Serve para um valor vazado ser reconhecível: um varredor de repositórios identifica
   * "rw_" e avisa, e quem encontra a string num log sabe o que é em vez de tratá-la como
   * texto qualquer.
   */
  private static final String CLIENT_ID_PREFIX = "rw_id_";
  private static final String SECRET_PREFIX = "rw_sk_";

  private final ApiClientRepository repository;
  private final IntegrationAuditService audit;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();
  private final long expirationMs;

  public ApiClientServiceImpl(
    ApiClientRepository repository,
    IntegrationAuditService audit,
    JwtService jwtService,
    @Value("${routewise.jwt.expiration-ms}") long expirationMs
  ) {
    this.repository = repository;
    this.audit = audit;
    this.jwtService = jwtService;
    this.expirationMs = expirationMs;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ApiClientDto> findAll() {
    return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
  }

  @Override
  public ApiClientCreatedDto create(ApiClientRequestDto request) {
    String clientId = generate(CLIENT_ID_PREFIX);
    String secret = generate(SECRET_PREFIX);

    ApiClient entity = new ApiClient(
      UUID.randomUUID(),
      request.name().trim(),
      clientId,
      passwordEncoder.encode(secret),
      String.join(",", request.scopes()),
      request.status()
    );

    return new ApiClientCreatedDto(toDto(repository.save(entity)), secret);
  }

  @Override
  public ApiClientDto update(UUID id, ApiClientRequestDto request) {
    ApiClient entity = findOrThrow(id);
    entity.setName(request.name().trim());
    entity.setScopes(String.join(",", request.scopes()));
    entity.setStatus(request.status());
    return toDto(repository.save(entity));
  }

  @Override
  public ApiClientCreatedDto regenerateSecret(UUID id) {
    ApiClient entity = findOrThrow(id);
    String secret = generate(SECRET_PREFIX);
    entity.setSecretHash(passwordEncoder.encode(secret));
    return new ApiClientCreatedDto(toDto(repository.save(entity)), secret);
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Integração não encontrada: " + id);
    }
    // O log de acesso aponta para o cliente com ON DELETE SET NULL: o histórico fica,
    // sem dono. Apagar a integração não deveria apagar o rastro do que ela acessou.
    repository.deleteById(id);
  }

  @Override
  public IntegrationTokenResponseDto issueToken(IntegrationTokenRequestDto request, String path) {
    ApiClient client = repository.findByClientId(request.clientId().trim()).orElse(null);

    // Cliente inexistente e segredo errado respondem igual: distinguir os dois diria a
    // quem está tentando que aquele clientId existe.
    if (client == null || !passwordEncoder.matches(request.clientSecret(), client.getSecretHash())) {
      audit.record(
        client == null ? null : client.getId(),
        request.clientId(),
        "POST", path,
        IntegrationRequestLog.OUTCOME_BAD_CREDENTIALS, 401
      );
      throw new IntegrationAuthException(BAD_CREDENTIALS);
    }

    if (!client.isActive()) {
      audit.record(
        client.getId(), request.clientId(),
        "POST", path,
        IntegrationRequestLog.OUTCOME_REVOKED, 403
      );
      throw new IntegrationRevokedException("Esta integração foi revogada.");
    }

    client.setLastUsedAt(Instant.now());
    repository.save(client);

    audit.record(
      client.getId(), client.getClientId(),
      "POST", path,
      IntegrationRequestLog.OUTCOME_SUCCESS, 200
    );

    List<String> scopes = List.copyOf(client.getScopeSet());
    return new IntegrationTokenResponseDto(
      jwtService.generateIntegrationToken(client.getClientId(), client.getScopeSet()),
      TOKEN_TYPE,
      expirationMs / 1000,
      scopes
    );
  }

  @Override
  @Transactional(readOnly = true)
  public ApiClient requireActive(String clientId) {
    return repository.findByClientId(clientId)
      .filter(ApiClient::isActive)
      .orElseThrow(() -> new IntegrationAuthException(BAD_CREDENTIALS));
  }

  /** 32 bytes de {@link SecureRandom} em base64url — o mesmo padrão do código de rota. */
  private String generate(String prefix) {
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private ApiClient findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Integração não encontrada: " + id));
  }

  private ApiClientDto toDto(ApiClient entity) {
    return new ApiClientDto(
      entity.getId(),
      entity.getName(),
      entity.getClientId(),
      List.copyOf(entity.getScopeSet()),
      entity.getStatus(),
      entity.getLastUsedAt(),
      entity.getCreatedAt()
    );
  }
}
