package com.routewise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request body for creating/updating an order. */
public record OrderRequestDto(

  @NotNull(message = "Selecione o cliente.")
  UUID customerId,

  @NotNull(message = "Selecione o veículo.")
  UUID vehicleId,

  @NotNull(message = "Selecione o motorista.")
  UUID driverId,

  @NotNull(message = "Selecione a forma de pagamento.")
  UUID paymentMethodId,

  @NotNull(message = "Selecione a condição de pagamento.")
  UUID paymentConditionId,

  @NotNull(message = "Informe a data.")
  LocalDate date,

  @NotNull(message = "Selecione o status.")
  @Pattern(regexp = "^(aguardando|faturado|em_rota|entregue|cancelado)$", message = "Status inválido.")
  String status,

  @NotEmpty(message = "Adicione ao menos um item.")
  @Valid
  List<OrderItemRequestDto> items,

  @NotNull(message = "Informe o desconto.")
  @PositiveOrZero(message = "O desconto não pode ser negativo.")
  Double discount,

  @NotNull(message = "Informe o frete.")
  @PositiveOrZero(message = "O frete não pode ser negativo.")
  Double shippingCost,

  @Size(max = 500, message = "Máximo de 500 caracteres.")
  String notes

) {}
