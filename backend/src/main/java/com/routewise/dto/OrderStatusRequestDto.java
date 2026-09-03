package com.routewise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Request body for changing only an order's status (see IOrderService#changeStatus). */
public record OrderStatusRequestDto(

  @NotNull(message = "Selecione o status.")
  @Pattern(regexp = "^(aguardando|faturado|em_rota|entregue|cancelado)$", message = "Status inválido.")
  String status

) {}
