package com.routewise.dto;

import java.util.UUID;

/** Response shape for an order line item. */
public record OrderItemDto(UUID productId, int quantity, double unitPrice) {
}
