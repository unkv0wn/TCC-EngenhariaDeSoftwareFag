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
@Table(name = "customers")
public class Customer {

  @Id
  private UUID id;

  @Column(name = "person_type", nullable = false, length = 10)
  private String personType;

  @Column(nullable = false, length = 20)
  private String document;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "trade_name", length = 150)
  private String tradeName;

  @Column(nullable = false, length = 12)
  private String type;

  @Column(nullable = false, length = 150)
  private String email;

  @Column(nullable = false, length = 20)
  private String phone;

  @Column(name = "address_zip_code", nullable = false, length = 10)
  private String addressZipCode;

  @Column(name = "address_street", nullable = false, length = 150)
  private String addressStreet;

  @Column(name = "address_number", nullable = false, length = 20)
  private String addressNumber;

  @Column(name = "address_complement", length = 100)
  private String addressComplement;

  @Column(name = "address_district", nullable = false, length = 100)
  private String addressDistrict;

  @Column(name = "address_city", nullable = false, length = 100)
  private String addressCity;

  @Column(name = "address_state", nullable = false, length = 2)
  private String addressState;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Customer() {
    // JPA
  }

  public Customer(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getPersonType() {
    return personType;
  }

  public void setPersonType(String personType) {
    this.personType = personType;
  }

  public String getDocument() {
    return document;
  }

  public void setDocument(String document) {
    this.document = document;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTradeName() {
    return tradeName;
  }

  public void setTradeName(String tradeName) {
    this.tradeName = tradeName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddressZipCode() {
    return addressZipCode;
  }

  public void setAddressZipCode(String addressZipCode) {
    this.addressZipCode = addressZipCode;
  }

  public String getAddressStreet() {
    return addressStreet;
  }

  public void setAddressStreet(String addressStreet) {
    this.addressStreet = addressStreet;
  }

  public String getAddressNumber() {
    return addressNumber;
  }

  public void setAddressNumber(String addressNumber) {
    this.addressNumber = addressNumber;
  }

  public String getAddressComplement() {
    return addressComplement;
  }

  public void setAddressComplement(String addressComplement) {
    this.addressComplement = addressComplement;
  }

  public String getAddressDistrict() {
    return addressDistrict;
  }

  public void setAddressDistrict(String addressDistrict) {
    this.addressDistrict = addressDistrict;
  }

  public String getAddressCity() {
    return addressCity;
  }

  public void setAddressCity(String addressCity) {
    this.addressCity = addressCity;
  }

  public String getAddressState() {
    return addressState;
  }

  public void setAddressState(String addressState) {
    this.addressState = addressState;
  }
}
