package com.routewise.service;

import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.MobileRouteSummaryDto;
import com.routewise.dto.RouteEventDto;

import java.util.List;
import java.util.UUID;

/**
 * Superfície do app do motorista.
 *
 * <p>Todo método recebe o {@code driverId} da sessão, e não do corpo ou do caminho: é o
 * que amarra cada operação ao motorista que o token prova ser. Antes da tela inicial,
 * qualquer motorista autenticado conseguia abrir qualquer rota sabendo o código.
 */
public interface IMobileRouteService {

  /** As rotas deste motorista para a tela inicial: as em aberto e o histórico recente. */
  List<MobileRouteSummaryDto> findByDriver(UUID driverId);

  MobileRouteDto findByCode(String accessCode, UUID driverId);

  /** Encerra a rota (status {@code concluida}). Idempotente: repetir devolve a rota já fechada. */
  MobileRouteDto finish(String accessCode, UUID driverId);

  RouteEventDto createEvent(String accessCode, UUID driverId, MobileEventRequestDto request);

  List<MobileEventBatchResultDto> createEvents(
    String accessCode,
    UUID driverId,
    List<MobileEventRequestDto> requests
  );
}
