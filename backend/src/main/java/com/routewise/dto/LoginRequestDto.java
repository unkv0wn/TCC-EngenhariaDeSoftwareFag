package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;

/** Login do painel. A senha passou a ser obrigatória — antes nem chegava aqui. */
public record LoginRequestDto(

  @NotBlank(message = "Informe o e-mail.")
  String email,

  @NotBlank(message = "Informe a senha.")
  String password

) {}
