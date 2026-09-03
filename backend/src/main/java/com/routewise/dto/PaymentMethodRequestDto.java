package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating/updating a payment method. */
public record PaymentMethodRequestDto(

  @NotBlank(message = "Informe o nome.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String name

) {}
