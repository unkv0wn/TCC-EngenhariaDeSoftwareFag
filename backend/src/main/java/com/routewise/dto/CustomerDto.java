package com.routewise.dto;

import java.util.UUID;

/** Response shape for a customer. */
public record CustomerDto(
  UUID id,
  String personType,
  String document,
  String name,
  String tradeName,
  String type,
  String email,
  String phone,
  AddressDto address
) {}
