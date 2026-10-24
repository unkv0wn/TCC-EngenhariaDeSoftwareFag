package com.routewise.controller;

import com.routewise.dto.IntegrationTokenRequestDto;
import com.routewise.dto.IntegrationTokenResponseDto;
import com.routewise.service.IApiClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Troca credencial por token — o {@code client_credentials} do OAuth 2, em forma mínima.
 *
 * <p>É o único caminho sob {@code /api/integration/**} que o filtro deixa passar sem
 * token: é aqui que o token nasce.
 *
 * <p>Por que não uma chave de API fixa no cabeçalho, que seria mais simples: uma chave
 * longeva vazada vale para sempre e só morre quando alguém percebe. Aqui o segredo é
 * usado uma vez por hora e o que circula em cada chamada é um token curto — vazamento de
 * token expira sozinho, e revogar o cliente corta o próximo.
 */
@RestController
@RequestMapping("/api/integration/token")
public class IntegrationTokenController {

  private final IApiClientService service;

  public IntegrationTokenController(IApiClientService service) {
    this.service = service;
  }

  @PostMapping
  public IntegrationTokenResponseDto issue(
    @Valid @RequestBody IntegrationTokenRequestDto request,
    HttpServletRequest httpRequest
  ) {
    return service.issueToken(request, httpRequest.getRequestURI());
  }
}
