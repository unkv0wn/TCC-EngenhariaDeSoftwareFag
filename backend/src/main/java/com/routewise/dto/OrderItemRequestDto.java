package com.routewise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** Request body for one order line item. */
public record OrderItemRequestDto(

  @NotNull(message = "Selecione o produto.")
  UUID productId,

  @NotNull(message = "Informe a quantidade.")
  @Positive(message = "Informe uma quantidade válida.")
  Integer quantity,

  @NotNull(message = "Informe o preço.")
  @Positive(message = "Informe um preço válido.")
  Double unitPrice

) {}
