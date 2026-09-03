package com.routewise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for creating/updating a customer. */
public record CustomerRequestDto(

  @NotBlank(message = "Selecione o tipo de pessoa.")
  @Pattern(regexp = "^(fisica|juridica)$", message = "Tipo de pessoa inválido.")
  String personType,

  @NotBlank(message = "Informe o documento.")
  @Size(max = 20, message = "Documento inválido.")
  String document,

  @NotBlank(message = "Informe o nome.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String name,

  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String tradeName,

  @NotBlank(message = "Selecione o tipo.")
  @Pattern(regexp = "^(cliente|fornecedor|ambos)$", message = "Tipo inválido.")
  String type,

  @NotBlank(message = "Informe o e-mail.")
  @Email(message = "Informe um e-mail válido.")
  @Size(max = 150, message = "Máximo de 150 caracteres.")
  String email,

  @NotBlank(message = "Informe o telefone.")
  @Size(max = 20, message = "Telefone inválido.")
  String phone,

  @NotNull(message = "Informe o endereço.")
  @Valid
  AddressDto address

) {}
