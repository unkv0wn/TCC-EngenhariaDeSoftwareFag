package com.routewise.service;

/**
 * Os status que uma rota salva pode ter.
 *
 * <p>Constantes, e não enum, porque a coluna é {@code VARCHAR} e o valor viaja como string
 * até o front (onde {@code RouteStatusFilter} usa os mesmos nomes). O que isto resolve é
 * só o literal solto: o encerramento pelo app e a máquina de transições em
 * {@link SavedRouteServiceImpl} precisam concordar sobre como se escreve "concluida", e
 * um erro de digitação entre os dois não daria erro de compilação.
 */
public final class RouteStatus {

  /** Nasce assim no salvamento, antes do caminhão sair. */
  public static final String PLANEJADA = "planejada";

  /** O caminhão saiu; é o estado em que o motorista registra eventos. */
  public static final String EM_ROTA = "em_rota";

  /** Encerrada — pelo gestor na web ou pelo motorista no app. Não aceita mais eventos. */
  public static final String CONCLUIDA = "concluida";

  private RouteStatus() {
  }
}
