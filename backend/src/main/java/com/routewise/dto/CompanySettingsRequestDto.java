package com.routewise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo pra atualizar as configurações da empresa. */
public record CompanySettingsRequestDto(

  @NotBlank(message = "Informe o nome da empresa.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String companyName,

  @Size(max = 10, message = "CEP inválido.")
  String addressZipCode,

  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String addressStreet,

  @Size(max = 20, message = "Máximo de 20 caracteres.")
  String addressNumber,

  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String addressDistrict,

  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String addressCity,

  @Size(max = 2, message = "UF inválida.")
  String addressState,

  @NotNull(message = "Defina a localização do depósito.")
  @DecimalMin(value = "-90.0", message = "Latitude inválida.")
  @DecimalMax(value = "90.0", message = "Latitude inválida.")
  Double latitude,

  @NotNull(message = "Defina a localização do depósito.")
  @DecimalMin(value = "-180.0", message = "Longitude inválida.")
  @DecimalMax(value = "180.0", message = "Longitude inválida.")
  Double longitude

) {}
