package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request body for creating/updating a product. `unit` is the referenced unit's id. */
public record ProductRequestDto(

  @NotBlank(message = "Informe o código.")
  @Size(max = 30, message = "Máximo de 30 caracteres.")
  String sku,

  @NotBlank(message = "Informe o nome.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String name,

  @Size(max = 200, message = "Máximo de 200 caracteres.")
  String description,

  @NotNull(message = "Selecione a unidade.")
  UUID unit,

  @NotNull(message = "Informe o preço.")
  @Positive(message = "Informe um preço válido.")
  Double unitPrice,

  @NotNull(message = "Informe o peso.")
  @Positive(message = "Informe um peso válido.")
  Double weightKg

) {}
