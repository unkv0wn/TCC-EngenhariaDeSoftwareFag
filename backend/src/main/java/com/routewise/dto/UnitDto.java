package com.routewise.dto;

import java.util.UUID;

/** Response shape for a unit of measure. */
public record UnitDto(UUID id, String code, String name) {
}
