package com.routewise.dto;

/**
 * Custo estimado de uma rota, aberto por componente e com a origem de cada insumo.
 *
 * <p>É <strong>estimativa do modelo do TCC</strong>, não despesa registrada: o sistema não
 * cadastra troca de pneu nem valor-hora de motorista. Os campos {@code *Measured} existem
 * para a tela poder dizer qual número foi medido na frota e qual veio de constante — o
 * mesmo total significa coisas diferentes nos dois casos, e apresentar um valor assumido
 * como medido é pior do que não mostrar valor nenhum.
 *
 * @param fuelLiters           litros estimados para a rota
 * @param fuelCost             R$ de combustível
 * @param tireCost             R$ de desgaste de pneu (constante do experimento)
 * @param driverCost           R$ de hora de motorista (constante do experimento)
 * @param total                soma dos três
 * @param perKm                total ÷ distância; zero quando a rota não tem distância
 * @param distanceKm           distância total da rota
 * @param durationMin          duração total da rota
 * @param cargoWeightKg        peso da carga entregue; zero também quando não há como somar
 *                             (rota antiga sem vínculo com pedido, ou produto sem peso)
 * @param profileLabel         perfil de custo aplicado, pela capacidade do veículo
 * @param capacityKg           capacidade do perfil, para dar sentido ao peso acima
 * @param consumptionLPer100Km consumo usado no cálculo
 * @param consumptionMeasured  se o consumo veio dos abastecimentos do veículo
 * @param fuelPricePerLiter    preço do litro usado no cálculo
 * @param fuelPriceMeasured    se o preço veio do que a frota pagou
 */
public record RouteCostDto(
  double fuelLiters,
  double fuelCost,
  double tireCost,
  double driverCost,
  double total,
  double perKm,
  double distanceKm,
  double durationMin,
  double cargoWeightKg,
  String profileLabel,
  double capacityKg,
  double consumptionLPer100Km,
  boolean consumptionMeasured,
  double fuelPricePerLiter,
  boolean fuelPriceMeasured
) {}
