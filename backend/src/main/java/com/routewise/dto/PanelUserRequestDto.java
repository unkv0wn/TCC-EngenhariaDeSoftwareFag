package com.routewise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Criação/edição de um acesso ao painel. */
public record PanelUserRequestDto(

  @NotBlank(message = "Informe o nome.")
  @Size(max = 120, message = "Máximo de 120 caracteres.")
  String name,

  @NotBlank(message = "Informe o e-mail.")
  @Email(message = "E-mail inválido.")
  @Size(max = 160, message = "Máximo de 160 caracteres.")
  String email,

  /**
   * Sem tamanho mínimo aqui de propósito: em branco na edição significa "manter a atual",
   * e uma anotação não sabe distinguir criação de edição. O mínimo é conferido no
   * serviço, onde essa diferença existe.
   */
  String password,

  @NotBlank(message = "Informe o status.")
  @Pattern(regexp = "^(ativo|bloqueado)$", message = "Status inválido.")
  String status

) {}
