package com.routewise.controller;

import com.routewise.dto.MobileUserDto;
import com.routewise.dto.MobileUserRequestDto;
import com.routewise.service.IMobileUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Acessos do app, do lado do gestor.
 *
 * <p>Fica fora de {@code /api/mobile/**} de propósito, apesar do nome: aquele prefixo é a
 * superfície do celular e passa pelo filtro de token do motorista. Isto é cadastro do
 * painel, e mora com o resto do CRUD.
 */
@RestController
@RequestMapping("/api/mobile-users")
public class MobileUserController {

  private final IMobileUserService service;

  public MobileUserController(IMobileUserService service) {
    this.service = service;
  }

  @GetMapping
  public List<MobileUserDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MobileUserDto create(@Valid @RequestBody MobileUserRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public MobileUserDto update(@PathVariable UUID id, @Valid @RequestBody MobileUserRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
