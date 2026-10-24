package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;

/** Troca de credencial por token — o `client_credentials` do OAuth 2, em forma mínima. */
public record IntegrationTokenRequestDto(

  @NotBlank(message = "Informe o clientId.")
  String clientId,

  @NotBlank(message = "Informe o clientSecret.")
  String clientSecret

) {}
