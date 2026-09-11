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
 * Rota gerada e salva. `driverId`/`vehicleId` ficam como UUID simples (mesmo motivo
 * de Order): o front resolve nome/placa cruzando com as listas já carregadas.
 *
 * <p>{@code result} e {@code stops} são JSON opaco pro backend — string crua guardada
 * numa coluna {@code jsonb} (passthrough, sem desserializar). Quem interpreta é o front.
 */
@Entity
@Table(name = "routes")
public class SavedRoute {

  @Id
  private UUID id;

  @Column(name = "driver_id", nullable = false)
  private UUID driverId;

  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(name = "departure_time", nullable = false, length = 5)
  private String departureTime;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "orders_count", nullable = false)
  private int ordersCount;

  @Column(name = "total_value", nullable = false)
  private double totalValue;

  @Column(name = "total_distance_km", nullable = false)
  private double totalDistanceKm;

  @Column(name = "total_duration_min", nullable = false)
  private double totalDurationMin;

  @Column(name = "completed_stops", nullable = false)
  private int completedStops;

  @Column(name = "result_json", nullable = false, columnDefinition = "text")
  private String result;

  @Column(name = "stops_json", nullable = false, columnDefinition = "text")
  private String stops;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SavedRoute() {
    // JPA
  }

  public SavedRoute(UUID id) {
    this.id = id;
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

  public UUID getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(UUID vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getDepartureTime() {
    return departureTime;
  }

  public void setDepartureTime(String departureTime) {
    this.departureTime = departureTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getOrdersCount() {
    return ordersCount;
  }

  public void setOrdersCount(int ordersCount) {
    this.ordersCount = ordersCount;
  }

  public double getTotalValue() {
    return totalValue;
  }

  public void setTotalValue(double totalValue) {
    this.totalValue = totalValue;
  }

  public double getTotalDistanceKm() {
    return totalDistanceKm;
  }

  public void setTotalDistanceKm(double totalDistanceKm) {
    this.totalDistanceKm = totalDistanceKm;
  }

  public double getTotalDurationMin() {
    return totalDurationMin;
  }

  public void setTotalDurationMin(double totalDurationMin) {
    this.totalDurationMin = totalDurationMin;
  }

  public int getCompletedStops() {
    return completedStops;
  }

  public void setCompletedStops(int completedStops) {
    this.completedStops = completedStops;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getStops() {
    return stops;
  }

  public void setStops(String stops) {
    this.stops = stops;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
