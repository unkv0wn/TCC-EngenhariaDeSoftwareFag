package com.routewise.controller;

import com.routewise.dto.MobileLoginRequestDto;
import com.routewise.dto.MobileLoginResponseDto;
import com.routewise.service.IMobileUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login do app do motorista.
 *
 * <p>Separado de {@link AuthController} (o do painel) porque as duas autenticações não se
 * parecem: a do painel emite token para qualquer e-mail, sem senha nem tabela de usuário;
 * esta confere senha com BCrypt e exige que o aparelho esteja cadastrado.
 *
 * <p>É o único caminho sob {@code /api/mobile/**} que o filtro de token deixa passar sem
 * sessão — é aqui que a sessão nasce.
 */
@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

  private final IMobileUserService service;

  public MobileAuthController(IMobileUserService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public MobileLoginResponseDto login(@Valid @RequestBody MobileLoginRequestDto request) {
    return service.authenticate(request);
  }
}
