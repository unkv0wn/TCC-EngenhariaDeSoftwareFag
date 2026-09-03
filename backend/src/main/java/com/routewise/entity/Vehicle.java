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
@Table(name = "vehicles")
public class Vehicle {

  @Id
  private UUID id;

  @Column(nullable = false, length = 10)
  private String plate;

  @Column(nullable = false, length = 100)
  private String model;

  @Column(nullable = false, length = 100)
  private String brand;

  @Column(nullable = false)
  private int year;

  @Column(nullable = false, length = 50)
  private String color;

  @Column(name = "capacity_kg", nullable = false)
  private double capacityKg;

  /** diesel | gasolina | etanol | eletrico — mesmos valores do enum do front (vehicle.ts). */
  @Column(name = "fuel_type", nullable = false, length = 10)
  private String fuelType;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Vehicle() {
    // JPA
  }

  public Vehicle(UUID id, String plate, String model, String brand, int year, String color, double capacityKg, String fuelType) {
    this.id = id;
    this.plate = plate;
    this.model = model;
    this.brand = brand;
    this.year = year;
    this.color = color;
    this.capacityKg = capacityKg;
    this.fuelType = fuelType;
  }

  public UUID getId() {
    return id;
  }

  public String getPlate() {
    return plate;
  }

  public void setPlate(String plate) {
    this.plate = plate;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public double getCapacityKg() {
    return capacityKg;
  }

  public void setCapacityKg(double capacityKg) {
    this.capacityKg = capacityKg;
  }

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }
}
