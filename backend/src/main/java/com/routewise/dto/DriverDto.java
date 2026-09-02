package com.routewise.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Response shape for a driver. */
public record DriverDto(
  UUID id,
  String fullName,
  String cpf,
  String phone,
  String cnhNumber,
  String cnhCategory,
  LocalDate cnhValidity,
  String status
) {}
