package com.routewise.service;

import com.routewise.dto.RouteCostDto;
import com.routewise.dto.SavedRouteDto;
import com.routewise.dto.SavedRouteRequestDto;

import java.util.List;
import java.util.UUID;

public interface ISavedRouteService {

  List<SavedRouteDto> findAll();

  SavedRouteDto findById(UUID id);

  SavedRouteDto create(SavedRouteRequestDto request);

  void delete(UUID id);

  /** Muda só o status, validando a máquina de estados (ver VALID_STATUS_TRANSITIONS). */
  SavedRouteDto changeStatus(UUID id, String status);

  /**
   * Emite um código de acesso novo para a rota, descartando o anterior.
   *
   * <p>Serve a dois casos: rotas criadas antes da V17, que nasceram sem código, e revogação
   * — se o link vazou, emitir outro invalida o que estava circulando, já que o código é a
   * única credencial.
   */
  SavedRouteDto regenerateAccessCode(UUID id);

  /**
   * Custo estimado desta rota pelo modelo do TCC.
   *
   * <p>Fica no serviço da rota, e não num serviço de custo próprio, porque a pergunta é
   * sobre a rota: quem a tem em mãos deve conseguir perguntar quanto ela custou sem ter
   * de saber que existe um estimador atrás.
   */
  RouteCostDto cost(UUID id);
}
