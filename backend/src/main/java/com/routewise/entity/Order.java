package com.routewise.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * `customerId`/`vehicleId`/`driverId`/`paymentMethodId`/`paymentConditionId` ficam como UUID
 * simples (não `@ManyToOne`) pelo mesmo motivo de `Refueling`/`Product`: evita N+1/lazy-loading
 * só pra exibir o id — quem precisa do nome/dado resolve isso no front, cruzando com as listas
 * já carregadas de cada cadastro.
 */
@Entity
@Table(name = "orders")
public class Order {

  @Id
  private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(name = "driver_id", nullable = false)
  private UUID driverId;

  @Column(name = "payment_method_id", nullable = false)
  private UUID paymentMethodId;

  @Column(name = "payment_condition_id", nullable = false)
  private UUID paymentConditionId;

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private double discount;

  @Column(name = "shipping_cost", nullable = false)
  private double shippingCost;

  @Column(length = 500)
  private String notes;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "item_order")
  private List<OrderItem> items = new ArrayList<>();

  // Sem @OrderColumn aqui de propósito — a ordem que importa é cronológica (changedAt),
  // não a ordem de inserção física da tabela.
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "order_status_history", joinColumns = @JoinColumn(name = "order_id"))
  @OrderBy("changedAt asc")
  private List<OrderHistoryEntry> history = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Order() {
    // JPA
  }

  public Order(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public UUID getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(UUID vehicleId) {
    this.vehicleId = vehicleId;
  }

  public UUID getDriverId() {
    return driverId;
  }

  public void setDriverId(UUID driverId) {
    this.driverId = driverId;
  }

  public UUID getPaymentMethodId() {
    return paymentMethodId;
  }

  public void setPaymentMethodId(UUID paymentMethodId) {
    this.paymentMethodId = paymentMethodId;
  }

  public UUID getPaymentConditionId() {
    return paymentConditionId;
  }

  public void setPaymentConditionId(UUID paymentConditionId) {
    this.paymentConditionId = paymentConditionId;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public double getDiscount() {
    return discount;
  }

  public void setDiscount(double discount) {
    this.discount = discount;
  }

  public double getShippingCost() {
    return shippingCost;
  }

  public void setShippingCost(double shippingCost) {
    this.shippingCost = shippingCost;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public List<OrderHistoryEntry> getHistory() {
    return history;
  }
}
