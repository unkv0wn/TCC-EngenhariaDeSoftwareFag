package com.routewise.service;

import com.routewise.dto.LoginRequestDto;
import com.routewise.dto.LoginResponseDto;
import com.routewise.dto.PanelUserDto;
import com.routewise.dto.PanelUserRequestDto;

import java.util.List;
import java.util.UUID;

public interface IPanelUserService {

  List<PanelUserDto> findAll();

  PanelUserDto create(PanelUserRequestDto request);

  PanelUserDto update(UUID id, PanelUserRequestDto request);

  void delete(UUID id, UUID requesterId);

  /** Confere a credencial e devolve o token da sessão. */
  LoginResponseDto authenticate(LoginRequestDto request);
}
