package com.routewise.service;

import com.routewise.dto.MobileLoginRequestDto;
import com.routewise.dto.MobileLoginResponseDto;
import com.routewise.dto.MobileUserDto;
import com.routewise.dto.MobileUserRequestDto;

import java.util.List;
import java.util.UUID;

public interface IMobileUserService {

  List<MobileUserDto> findAll();

  MobileUserDto create(MobileUserRequestDto request);

  MobileUserDto update(UUID id, MobileUserRequestDto request);

  void delete(UUID id);

  /** Confere credencial e aparelho e devolve a sessão do motorista. */
  MobileLoginResponseDto authenticate(MobileLoginRequestDto request);
}
