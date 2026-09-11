package com.routewise.dto;

/** Configurações da empresa. */
public record CompanySettingsDto(
  String companyName,
  String addressZipCode,
  String addressStreet,
  String addressNumber,
  String addressDistrict,
  String addressCity,
  String addressState,
  double latitude,
  double longitude
) {}
