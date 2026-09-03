package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Um fato imutável da linha do tempo de status de um pedido — vive dentro de Order.history. */
@Embeddable
public class OrderHistoryEntry {

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  protected OrderHistoryEntry() {
    // JPA
  }

  public OrderHistoryEntry(String status, Instant changedAt) {
    this.status = status;
    this.changedAt = changedAt;
  }

  public String getStatus() {
    return status;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
