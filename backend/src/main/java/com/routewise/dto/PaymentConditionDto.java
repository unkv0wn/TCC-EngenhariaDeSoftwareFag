package com.routewise.dto;

import java.util.UUID;

/** Response shape for a payment condition. */
public record PaymentConditionDto(UUID id, String name, int installments, int intervalDays) {
}
