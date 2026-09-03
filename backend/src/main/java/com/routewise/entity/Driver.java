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
@Table(name = "drivers")
public class Driver {

  @Id
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(nullable = false, length = 14)
  private String cpf;

  @Column(nullable = false, length = 20)
  private String phone;

  @Column(name = "cnh_number", nullable = false, length = 11)
  private String cnhNumber;

  @Column(name = "cnh_category", nullable = false, length = 2)
  private String cnhCategory;

  @Column(name = "cnh_validity", nullable = false)
  private LocalDate cnhValidity;

  /** "ativo" | "inativo" — mesmos valores usados pelo enum do front (driver.ts). */
  @Column(nullable = false, length = 10)
  private String status;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Driver() {
    // JPA
  }

  public Driver(
    UUID id,
    String fullName,
    String cpf,
    String phone,
    String cnhNumber,
    String cnhCategory,
    LocalDate cnhValidity,
    String status
  ) {
    this.id = id;
    this.fullName = fullName;
    this.cpf = cpf;
    this.phone = phone;
    this.cnhNumber = cnhNumber;
    this.cnhCategory = cnhCategory;
    this.cnhValidity = cnhValidity;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getCpf() {
    return cpf;
  }

  public void setCpf(String cpf) {
    this.cpf = cpf;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getCnhNumber() {
    return cnhNumber;
  }

  public void setCnhNumber(String cnhNumber) {
    this.cnhNumber = cnhNumber;
  }

  public String getCnhCategory() {
    return cnhCategory;
  }

  public void setCnhCategory(String cnhCategory) {
    this.cnhCategory = cnhCategory;
  }

  public LocalDate getCnhValidity() {
    return cnhValidity;
  }

  public void setCnhValidity(LocalDate cnhValidity) {
    this.cnhValidity = cnhValidity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
