package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Acesso de um gestor ao painel.
 *
 * <p>Separado de {@link MobileUser} apesar da forma parecida: aquele é credencial de
 * campo, presa a um motorista e a um aparelho; este é de escritório. As duas não
 * compartilham ciclo de vida — bloquear alguém no painel não diz nada sobre o celular
 * dele —, e juntá-las numa tabela só significaria um monte de coluna nula de cada lado.
 */
@Entity
@Table(name = "panel_users")
public class PanelUser {

  /** Acesso liberado. */
  public static final String STATUS_ACTIVE = "ativo";

  /** Acesso suspenso — a credencial continua válida, mas não entra. */
  public static final String STATUS_BLOCKED = "bloqueado";

  @Id
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 160)
  private String email;

  /** BCrypt. A senha em claro não é guardada nem logada em lugar nenhum. */
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 10)
  private String status;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PanelUser() {
    // JPA
  }

  public PanelUser(UUID id, String name, String email, String passwordHash, String status) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.status = status;
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
