package com.routewise.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * O que a tela inicial do gestor mostra.
 *
 * <p>Calculado no servidor, e não somando listas no navegador, por dois motivos: o custo
 * depende do modelo empírico, que vive no backend, e trazer rotas, pedidos e
 * abastecimentos inteiros só para somá-los seria pagar a lista toda para mostrar seis
 * números.
 *
 * @param period     janela considerada
 * @param operation  o que a operação fez
 * @param cost       quanto o modelo diz que custou, e quanto foi de fato abastecido
 * @param vehicles   consumo por veículo, medido quando há abastecimento suficiente
 * @param deliveries entregas por dia, para o gráfico
 */
public record DashboardDto(
  Period period,
  Operation operation,
  Cost cost,
  List<VehicleConsumption> vehicles,
  List<DailyDeliveries> deliveries
) {

  public record Period(int days, LocalDate from, LocalDate to) {}

  /**
   * @param routes          rotas criadas na janela
   * @param routesConcluded quantas já foram encerradas
   * @param distanceKm      distância somada das rotas da janela
   * @param durationMin     duração somada
   * @param stops           paradas somadas
   * @param deliveries      pedidos entregues na janela (pela data de entrega real)
   * @param pendingOrders   pedidos em `faturado` — esperando entrar numa rota
   */
  public record Operation(
    int routes,
    int routesConcluded,
    double distanceKm,
    double durationMin,
    int stops,
    int deliveries,
    int pendingOrders
  ) {}

  /**
   * @param fuelLiters        litros estimados pelo modelo
   * @param fuelCost          R$ de combustível, estimado
   * @param tireCost          R$ de desgaste de pneu, estimado
   * @param driverCost        R$ de hora de motorista, estimado
   * @param total             soma dos três
   * @param perKm             total dividido pela distância
   * @param refueledLiters    litros de fato abastecidos na janela
   * @param refueledCost      R$ de fato gastos em combustível na janela
   * @param fuelPricePerLiter preço usado na estimativa
   * @param fuelPriceMeasured o preço veio dos abastecimentos, ou é a constante do modelo?
   */
  public record Cost(
    double fuelLiters,
    double fuelCost,
    double tireCost,
    double driverCost,
    double total,
    double perKm,
    double refueledLiters,
    double refueledCost,
    double fuelPricePerLiter,
    boolean fuelPriceMeasured
  ) {}

  /**
   * @param consumptionLPer100Km consumo usado para este veículo
   * @param measured             medido nos abastecimentos dele, ou constante do perfil
   * @param refuelings           quantos abastecimentos existem no histórico
   * @param routeDistanceKm      quanto ele rodou em rotas da janela
   */
  public record VehicleConsumption(
    String plate,
    String profileLabel,
    double consumptionLPer100Km,
    boolean measured,
    int refuelings,
    double routeDistanceKm
  ) {}

  public record DailyDeliveries(LocalDate date, int deliveries) {}
}
