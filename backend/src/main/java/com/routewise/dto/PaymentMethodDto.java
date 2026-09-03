package com.routewise.dto;

import java.util.UUID;

/** Response shape for a payment method. */
public record PaymentMethodDto(UUID id, String name) {
}
