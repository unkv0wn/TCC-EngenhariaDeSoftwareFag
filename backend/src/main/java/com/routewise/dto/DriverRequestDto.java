package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request body for creating/updating a driver. */
public record DriverRequestDto(

  @NotBlank(message = "Informe o nome completo.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String fullName,

  @NotBlank(message = "Informe o CPF.")
  @Size(max = 14, message = "CPF inválido.")
  String cpf,

  @NotBlank(message = "Informe o telefone.")
  @Size(max = 20, message = "Telefone inválido.")
  String phone,

  @NotBlank(message = "Informe o número da CNH.")
  @Size(max = 11, message = "Número de CNH inválido.")
  String cnhNumber,

  @NotBlank(message = "Informe a categoria da CNH.")
  @Pattern(regexp = "^(A|B|C|D|E|AB|AC|AD|AE)$", message = "Categoria de CNH inválida.")
  String cnhCategory,

  @NotNull(message = "Informe a validade da CNH.")
  LocalDate cnhValidity,

  @NotBlank(message = "Informe o status.")
  @Pattern(regexp = "^(ativo|inativo)$", message = "Status inválido.")
  String status

) {}
