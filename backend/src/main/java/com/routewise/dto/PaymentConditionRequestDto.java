package com.routewise.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for creating/updating a payment condition. */
public record PaymentConditionRequestDto(

  @NotBlank(message = "Informe o nome.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String name,

  @NotNull(message = "Informe o número de parcelas.")
  @Min(value = 1, message = "Informe ao menos 1 parcela.")
  Integer installments,

  @NotNull(message = "Informe o intervalo entre parcelas.")
  @Min(value = 0, message = "Informe um valor válido.")
  Integer intervalDays

) {}
