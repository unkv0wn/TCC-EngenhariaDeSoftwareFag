package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Criação/edição de um cliente de integração. */
public record ApiClientRequestDto(

  @NotBlank(message = "Informe o nome da integração.")
  @Size(max = 120, message = "Máximo de 120 caracteres.")
  String name,

  @NotEmpty(message = "Selecione ao menos um escopo.")
  List<@Pattern(
    regexp = "^(orders|routes|deliveries):read$",
    message = "Escopo inválido. Aceitos: orders:read, routes:read, deliveries:read."
  ) String> scopes,

  @NotBlank(message = "Informe o status.")
  @Pattern(regexp = "^(ativo|revogado)$", message = "Status inválido.")
  String status

) {}
