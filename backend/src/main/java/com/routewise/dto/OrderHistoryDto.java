package com.routewise.dto;

import java.time.Instant;

/** Response shape for one entry of an order's status timeline. */
public record OrderHistoryDto(String status, Instant changedAt) {
}
