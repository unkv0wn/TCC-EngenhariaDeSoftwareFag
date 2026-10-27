package com.routewise.controller;

import com.routewise.auth.PanelAuthFilter;
import com.routewise.auth.PanelSession;
import com.routewise.dto.LoginRequestDto;
import com.routewise.dto.LoginResponseDto;
import com.routewise.dto.PanelSessionDto;
import com.routewise.service.IPanelUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login do painel.
 *
 * <p>Este endpoint emitia token para qualquer e-mail que recebesse, sem conferir senha e
 * sem tabela de usuário — a única checagem de credencial morava num par fixo dentro do
 * front. Agora ele confere senha em BCrypt contra {@code panel_users}, e é o único
 * caminho do painel que o {@link PanelAuthFilter} deixa passar sem sessão.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final IPanelUserService service;

  public AuthController(IPanelUserService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
    return service.authenticate(request);
  }

  /**
   * Quem está logado nesta sessão.
   *
   * <p>Existe por causa do cookie {@code httpOnly}: a página não consegue abrir o token
   * para saber de quem é a sessão. Passa pelo filtro como qualquer outro caminho, então
   * responder 200 aqui é também a prova de que o cookie ainda vale.
   */
  @GetMapping("/me")
  public PanelSessionDto me(@RequestAttribute(PanelAuthFilter.SESSION_ATTRIBUTE) PanelSession session) {
    return new PanelSessionDto(session.userId(), session.name(), session.email());
  }
}
