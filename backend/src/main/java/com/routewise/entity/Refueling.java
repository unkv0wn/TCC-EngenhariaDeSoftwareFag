package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "refuelings")
public class Refueling {

  @Id
  private UUID id;

  /**
   * Guardado como UUID simples (não @ManyToOne) — a integridade referencial já é garantida
   * pela FK no banco (ver migration), e isso evita lazy-loading/N+1 desnecessário já que o
   * front só precisa do id pra cruzar com a lista de veículos/motoristas que já tem em mãos.
   */
  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(name = "driver_id", nullable = false)
  private UUID driverId;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "odometer_km", nullable = false)
  private double odometerKm;

  @Column(name = "liters_refueled", nullable = false)
  private double litersRefueled;

  @Column(name = "price_per_liter", nullable = false)
  private double pricePerLiter;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Refueling() {
    // JPA
  }

  public Refueling(
    UUID id,
    UUID vehicleId,
    UUID driverId,
    LocalDate date,
    double odometerKm,
    double litersRefueled,
    double pricePerLiter
  ) {
    this.id = id;
    this.vehicleId = vehicleId;
    this.driverId = driverId;
    this.date = date;
    this.odometerKm = odometerKm;
    this.litersRefueled = litersRefueled;
    this.pricePerLiter = pricePerLiter;
  }

  public UUID getId() {
    return id;
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

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public double getOdometerKm() {
    return odometerKm;
  }

  public void setOdometerKm(double odometerKm) {
    this.odometerKm = odometerKm;
  }

  public double getLitersRefueled() {
    return litersRefueled;
  }

  public void setLitersRefueled(double litersRefueled) {
    this.litersRefueled = litersRefueled;
  }

  public double getPricePerLiter() {
    return pricePerLiter;
  }

  public void setPricePerLiter(double pricePerLiter) {
    this.pricePerLiter = pricePerLiter;
  }
}
