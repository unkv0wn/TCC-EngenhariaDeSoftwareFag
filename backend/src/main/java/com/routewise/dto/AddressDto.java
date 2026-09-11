package com.routewise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Endereço aninhado — usado tanto na resposta quanto no corpo da requisição do cliente. */
public record AddressDto(

  @NotBlank(message = "Informe o CEP.")
  @Size(max = 10, message = "CEP inválido.")
  String zipCode,

  @NotBlank(message = "Informe o logradouro.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String street,

  @NotBlank(message = "Informe o número.")
  @Size(max = 20, message = "Máximo de 20 caracteres.")
  String number,

  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String complement,

  @NotBlank(message = "Informe o bairro.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String district,

  @NotBlank(message = "Informe a cidade.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String city,

  @NotBlank(message = "Selecione a UF.")
  @Size(min = 2, max = 2, message = "UF inválida.")
  String state,

  /** Coordenada opcional (geocoding). Só validada quando presente. */
  @DecimalMin(value = "-90.0", message = "Latitude inválida.")
  @DecimalMax(value = "90.0", message = "Latitude inválida.")
  Double latitude,

  @DecimalMin(value = "-180.0", message = "Longitude inválida.")
  @DecimalMax(value = "180.0", message = "Longitude inválida.")
  Double longitude

) {}
