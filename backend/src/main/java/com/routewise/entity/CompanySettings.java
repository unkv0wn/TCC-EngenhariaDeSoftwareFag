package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Configurações da empresa — linha única (id fixo em {@link #SINGLETON_ID}). */
@Entity
@Table(name = "company_settings")
public class CompanySettings {

  public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-0000000c0001");

  @Id
  private UUID id;

  @Column(name = "company_name", nullable = false, length = 150)
  private String companyName;

  @Column(name = "address_zip_code", length = 10)
  private String addressZipCode;

  @Column(name = "address_street", length = 150)
  private String addressStreet;

  @Column(name = "address_number", length = 20)
  private String addressNumber;

  @Column(name = "address_district", length = 100)
  private String addressDistrict;

  @Column(name = "address_city", length = 100)
  private String addressCity;

  @Column(name = "address_state", length = 2)
  private String addressState;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CompanySettings() {
    // JPA
  }

  public CompanySettings(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
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

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }
}
