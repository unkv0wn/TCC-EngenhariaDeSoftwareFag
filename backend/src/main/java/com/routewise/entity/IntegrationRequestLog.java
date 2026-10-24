package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Um acesso à superfície de integração, aceito ou recusado.
 *
 * <p>Imutável por construção: não há setter nenhum. Registro de acesso que alguém pode
 * editar não serve de evidência, e aqui isso é barato de garantir.
 */
@Entity
@Table(name = "integration_request_log")
public class IntegrationRequestLog {

  public static final String OUTCOME_SUCCESS = "sucesso";
  public static final String OUTCOME_BAD_CREDENTIALS = "credencial_invalida";
  public static final String OUTCOME_REVOKED = "cliente_revogado";
  public static final String OUTCOME_BAD_TOKEN = "token_invalido";
  public static final String OUTCOME_MISSING_SCOPE = "escopo_insuficiente";

  @Id
  private UUID id;

  /** Nulo quando a tentativa não tem dono — client_id que não existe, por exemplo. */
  @Column(name = "api_client_id")
  private UUID apiClientId;

  /** O client_id enviado, mesmo que não exista: é o que identifica quem tentou. */
  @Column(name = "client_id_tentado", length = 40)
  private String attemptedClientId;

  @Column(nullable = false, length = 10)
  private String method;

  @Column(nullable = false, length = 200)
  private String path;

  @Column(nullable = false, length = 30)
  private String outcome;

  @Column(name = "status_code", nullable = false)
  private int statusCode;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected IntegrationRequestLog() {
    // JPA
  }

  public IntegrationRequestLog(
    UUID apiClientId,
    String attemptedClientId,
    String method,
    String path,
    String outcome,
    int statusCode
  ) {
    this.id = UUID.randomUUID();
    this.apiClientId = apiClientId;
    this.attemptedClientId = attemptedClientId;
    this.method = method;
    this.path = path;
    this.outcome = outcome;
    this.statusCode = statusCode;
    this.occurredAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getApiClientId() {
    return apiClientId;
  }

  public String getAttemptedClientId() {
    return attemptedClientId;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public String getOutcome() {
    return outcome;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
