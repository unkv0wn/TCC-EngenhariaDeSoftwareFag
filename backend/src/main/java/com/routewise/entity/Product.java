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
 * `unitId` fica como UUID simples (não `@ManyToOne`) pelo mesmo motivo de `Refueling`
 * (vehicleId/driverId): evita N+1/lazy-loading só pra exibir o id da unidade — quem
 * precisa do nome/código da unidade resolve isso no front, cruzando com a lista de units.
 */
@Entity
@Table(name = "products")
public class Product {

  @Id
  private UUID id;

  @Column(nullable = false, length = 30)
  private String sku;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 200)
  private String description;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(name = "unit_price", nullable = false)
  private double unitPrice;

  @Column(name = "weight_kg", nullable = false)
  private double weightKg;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Product() {
    // JPA
  }

  public Product(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UUID getUnitId() {
    return unitId;
  }

  public void setUnitId(UUID unitId) {
    this.unitId = unitId;
  }

  public double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(double unitPrice) {
    this.unitPrice = unitPrice;
  }

  public double getWeightKg() {
    return weightKg;
  }

  public void setWeightKg(double weightKg) {
    this.weightKg = weightKg;
  }
}
