package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Credencial de um sistema que integra com o RouteWise.
 *
 * <p>Mesmo desenho da credencial do app do motorista — segredo só como hash BCrypt,
 * status que o gestor revoga, registro do último uso — mas para um sistema e não para uma
 * pessoa: não há aparelho a vincular, e no lugar disso há escopos, porque um integrador
 * costuma precisar de uma fatia dos dados e não de tudo.
 */
@Entity
@Table(name = "api_clients")
public class ApiClient {

  public static final String STATUS_ACTIVE = "ativo";
  public static final String STATUS_REVOKED = "revogado";

  @Id
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "client_id", nullable = false, length = 40)
  private String clientId;

  /** BCrypt. O segredo em claro existe uma vez, na resposta da criação, e nunca mais. */
  @Column(name = "secret_hash", nullable = false, length = 100)
  private String secretHash;

  /** Separados por vírgula — ver {@link #getScopeSet()}. */
  @Column(nullable = false, length = 255)
  private String scopes;

  @Column(nullable = false, length = 10)
  private String status;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ApiClient() {
    // JPA
  }

  public ApiClient(UUID id, String name, String clientId, String secretHash, String scopes, String status) {
    this.id = id;
    this.name = name;
    this.clientId = clientId;
    this.secretHash = secretHash;
    this.scopes = scopes;
    this.status = status;
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  /**
   * Os escopos como conjunto.
   *
   * <p>A coluna é texto separado por vírgula, então quem decide acesso não deve lê-la
   * crua: {@code "orders:read"} aparece dentro de {@code "orders:readonly"} numa busca
   * por substring, e um escopo que não existe passaria.
   */
  public Set<String> getScopeSet() {
    if (scopes == null || scopes.isBlank()) {
      return Set.of();
    }
    return new LinkedHashSet<>(Arrays.stream(scopes.split(","))
      .map(String::trim)
      .filter(scope -> !scope.isEmpty())
      .toList());
  }

  public boolean hasScope(String scope) {
    return getScopeSet().contains(scope);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getClientId() {
    return clientId;
  }

  public String getSecretHash() {
    return secretHash;
  }

  public void setSecretHash(String secretHash) {
    this.secretHash = secretHash;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
