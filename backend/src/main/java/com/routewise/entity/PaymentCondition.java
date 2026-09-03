package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_conditions")
public class PaymentCondition {

  @Id
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private int installments;

  @Column(name = "interval_days", nullable = false)
  private int intervalDays;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PaymentCondition() {
    // JPA
  }

  public PaymentCondition(UUID id) {
    this.id = id;
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

  public int getInstallments() {
    return installments;
  }

  public void setInstallments(int installments) {
    this.installments = installments;
  }

  public int getIntervalDays() {
    return intervalDays;
  }

  public void setIntervalDays(int intervalDays) {
    this.intervalDays = intervalDays;
  }
}
