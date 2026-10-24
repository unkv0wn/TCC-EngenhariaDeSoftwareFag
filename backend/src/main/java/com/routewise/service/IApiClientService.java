package com.routewise.service;

import com.routewise.dto.ApiClientCreatedDto;
import com.routewise.dto.ApiClientDto;
import com.routewise.dto.ApiClientRequestDto;
import com.routewise.dto.IntegrationTokenRequestDto;
import com.routewise.dto.IntegrationTokenResponseDto;
import com.routewise.entity.ApiClient;

import java.util.List;
import java.util.UUID;

public interface IApiClientService {

  List<ApiClientDto> findAll();

  /** Cria e devolve o segredo em claro — a única vez que ele existe fora do hash. */
  ApiClientCreatedDto create(ApiClientRequestDto request);

  ApiClientDto update(UUID id, ApiClientRequestDto request);

  /** Gera um segredo novo e invalida o anterior. Devolve o novo, também uma única vez. */
  ApiClientCreatedDto regenerateSecret(UUID id);

  void delete(UUID id);

  /** Troca credencial por token. Lança se a credencial não serve. */
  IntegrationTokenResponseDto issueToken(IntegrationTokenRequestDto request, String path);

  /** O cliente por trás de um token válido, para a checagem de escopo e o último uso. */
  ApiClient requireActive(String clientId);
}
