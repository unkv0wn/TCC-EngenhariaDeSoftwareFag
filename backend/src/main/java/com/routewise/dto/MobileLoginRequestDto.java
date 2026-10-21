package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login do app. O {@code deviceId} vem do próprio aparelho e é conferido contra o que o
 * gestor cadastrou — sem ele a senha bastaria, em qualquer celular.
 */
public record MobileLoginRequestDto(

  @NotBlank(message = "Informe o usuário.")
  @Size(max = 60, message = "Usuário inválido.")
  String username,

  @NotBlank(message = "Informe a senha.")
  @Size(max = 72, message = "Senha inválida.")
  String password,

  @NotBlank(message = "O aplicativo não conseguiu identificar este aparelho.")
  @Size(max = 64, message = "Identificador de aparelho inválido.")
  String deviceId

) {}
