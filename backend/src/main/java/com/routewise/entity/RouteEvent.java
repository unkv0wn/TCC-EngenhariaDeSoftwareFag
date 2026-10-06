package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Um acontecimento registrado durante a execução de uma rota.
 *
 * <p>{@code routeId} é UUID simples, como no resto do projeto — a FK com CASCADE existe
 * no schema, mas não há {@code @ManyToOne}: o serviço só precisa do id pra consultar e
 * validar, e mapear a associação traria carregamento em cascata sem ganho nenhum aqui.
 *
 * <p>{@code stopIndex} é nulo quando o evento não pertence a uma parada específica.
 * {@code occurredAt} é quando o fato aconteceu (informado por quem registra);
 * {@code createdAt} é quando foi registrado no sistema — os dois diferem quando o
 * motorista registra algo depois que já passou.
 */
@Entity
@Table(name = "route_events")
public class RouteEvent {

  @Id
  private UUID id;

  @Column(name = "route_id", nullable = false)
  private UUID routeId;

  @Column(name = "stop_index")
  private Integer stopIndex;

  @Column(nullable = false, length = 20)
  private String type;

  @Column(name = "author_role", nullable = false, length = 20)
  private String authorRole;

  @Column(name = "author_name", nullable = false, length = 120)
  private String authorName;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  /**
   * Id gerado no celular, pra fila offline do app do motorista ser idempotente.
   * Nulo nos eventos registrados pelo gestor na web, que não passam por fila.
   */
  @Column(name = "client_event_id")
  private UUID clientEventId;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  protected RouteEvent() {
    // JPA
  }

  public RouteEvent(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRouteId() {
    return routeId;
  }

  public void setRouteId(UUID routeId) {
    this.routeId = routeId;
  }

  public Integer getStopIndex() {
    return stopIndex;
  }

  public void setStopIndex(Integer stopIndex) {
    this.stopIndex = stopIndex;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAuthorRole() {
    return authorRole;
  }

  public void setAuthorRole(String authorRole) {
    this.authorRole = authorRole;
  }

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public UUID getClientEventId() {
    return clientEventId;
  }

  public void setClientEventId(UUID clientEventId) {
    this.clientEventId = clientEventId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
