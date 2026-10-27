package com.routewise.controller;

import com.routewise.auth.PanelAuthFilter;
import com.routewise.auth.PanelSession;
import com.routewise.dto.PanelUserDto;
import com.routewise.dto.PanelUserRequestDto;
import com.routewise.service.IPanelUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Acessos ao painel, administrados de dentro do próprio painel.
 *
 * <p>Fica atrás do {@link PanelAuthFilter} como todo o resto do CRUD — o que importa
 * aqui mais do que em qualquer outra tela: é o cadastro que concede acesso a todas as
 * outras.
 */
@RestController
@RequestMapping("/api/users")
public class PanelUserController {

  private final IPanelUserService service;

  public PanelUserController(IPanelUserService service) {
    this.service = service;
  }

  @GetMapping
  public List<PanelUserDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PanelUserDto create(@Valid @RequestBody PanelUserRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public PanelUserDto update(@PathVariable UUID id, @Valid @RequestBody PanelUserRequestDto request) {
    return service.update(id, request);
  }

  /** O autor vem da sessão, não do pedido: é o que impede alguém de excluir o próprio acesso. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
    @PathVariable UUID id,
    @RequestAttribute(PanelAuthFilter.SESSION_ATTRIBUTE) PanelSession session
  ) {
    service.delete(id, session.userId());
  }
}
