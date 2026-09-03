package com.routewise.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Response shape for an order. */
public record OrderDto(
  UUID id,
  UUID customerId,
  UUID vehicleId,
  UUID driverId,
  UUID paymentMethodId,
  UUID paymentConditionId,
  LocalDate date,
  String status,
  List<OrderItemDto> items,
  double discount,
  double shippingCost,
  String notes,
  List<OrderHistoryDto> history
) {}
