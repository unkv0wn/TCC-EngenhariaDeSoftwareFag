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
 * Acesso de um motorista ao app de campo.
 *
 * <p>O aparelho autorizado faz parte da credencial, não é um detalhe à parte: usuário e
 * senha sozinhos viajam por WhatsApp e print, e um galpão inteiro passaria a registrar
 * entrega pelo celular de quem tivesse a senha. Com o {@code deviceId} cadastrado, a
 * senha vazada não serve em outro aparelho.
 */
@Entity
@Table(name = "mobile_users")
public class MobileUser {

  /** Acesso liberado. */
  public static final String STATUS_ACTIVE = "ativo";

  /** Acesso suspenso pelo gestor — a credencial continua válida, mas não entra. */
  public static final String STATUS_BLOCKED = "bloqueado";

  @Id
  private UUID id;

  @Column(name = "driver_id", nullable = false)
  private UUID driverId;

  @Column(nullable = false, length = 60)
  private String username;

  /** BCrypt. A senha em claro não é guardada nem logada em lugar nenhum. */
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  /**
   * {@code Settings.Secure.ANDROID_ID} do celular, cadastrado pelo gestor a partir do que
   * o app mostra na tela de login. Nulo = aparelho ainda não informado, e aí o login é
   * recusado: é o que impede celular não cadastrado de entrar.
   */
  @Column(name = "device_id", length = 64)
  private String deviceId;

  /** Como o gestor reconhece o aparelho na lista ("Moto G54 do Carlos"). */
  @Column(name = "device_label", length = 80)
  private String deviceLabel;

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

  protected MobileUser() {
    // JPA
  }

  public MobileUser(
    UUID id,
    UUID driverId,
    String username,
    String passwordHash,
    String deviceId,
    String deviceLabel,
    String status
  ) {
    this.id = id;
    this.driverId = driverId;
    this.username = username;
    this.passwordHash = passwordHash;
    this.deviceId = deviceId;
    this.deviceLabel = deviceLabel;
    this.status = status;
  }

  /** Vale entrar: acesso ativo e aparelho já cadastrado. */
  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  public boolean hasDevice() {
    return deviceId != null && !deviceId.isBlank();
  }

  /** O aparelho que pediu login é o cadastrado? Comparação sem diferenciar maiúsculas:
   *  o ANDROID_ID é hexadecimal e o caso em que ele chega varia por fabricante. */
  public boolean matchesDevice(String candidate) {
    return hasDevice() && candidate != null && deviceId.equalsIgnoreCase(candidate.trim());
  }

  public UUID getId() {
    return id;
  }

  public UUID getDriverId() {
    return driverId;
  }

  public void setDriverId(UUID driverId) {
    this.driverId = driverId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getDeviceLabel() {
    return deviceLabel;
  }

  public void setDeviceLabel(String deviceLabel) {
    this.deviceLabel = deviceLabel;
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
}
