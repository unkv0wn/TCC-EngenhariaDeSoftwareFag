package com.routewise.dto;

import java.util.UUID;

/** Response shape for a product. `unit` is the referenced unit's id. */
public record ProductDto(
  UUID id,
  String sku,
  String name,
  String description,
  UUID unit,
  double unitPrice,
  double weightKg
) {}
