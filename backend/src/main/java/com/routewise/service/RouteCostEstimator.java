package com.routewise.service;

import com.routewise.entity.Vehicle;
import com.routewise.validation.CargoOccupancy;
import com.routewise.validation.CostMatrixBuilder;
import com.routewise.validation.RoadType;
import com.routewise.validation.VehicleProfile;
import org.springframework.stereotype.Component;

/**
 * Custo estimado de uma rota inteira, pelo mesmo modelo que o experimento do TCC.
 *
 * <p>Até aqui o modelo de custo empírico vivia só em {@code com.routewise.validation}:
 * ~50 experimentos que rodam por fora, escrevem relatório em {@code docs/} e nunca
 * encostam no sistema. A rota é otimizada por <strong>duração</strong>; quanto ela custa
 * em reais era pergunta respondida num relatório, não no produto. Esta classe é a ponte.
 *
 * <p>A fórmula é a do {@link CostMatrixBuilder}, aplicada ao total da rota em vez de
 * aresta a aresta:
 *
 * <pre>
 *   loadFactor  = min(cargaKg / capacidadeKg, 1)
 *   consumo     = consumoBase * (1 + 0,30 * loadFactor)
 *   litros      = distanciaKm * consumo / 100
 *   desgaste    = distanciaKm * desgastePorKm(perfil) * (1 + 0,50 * loadFactor) * R$/pneu
 *   motorista   = horas * R$/hora
 *   total       = litros * R$/litro + desgaste + motorista
 * </pre>
 *
 * <p>Duas diferenças em relação ao experimento, ambas por falta de dado e não por
 * escolha de modelagem:
 *
 * <ul>
 *   <li><strong>Tipo de via.</strong> O experimento multiplica consumo e desgaste pelo
 *       {@link RoadType} de cada trecho. Uma rota salva guarda só os totais — não há
 *       de onde saber a mistura de via —, então vale {@code ARTERIAL}, que é a
 *       referência (multiplicadores 1,0). Rota urbana é subestimada, rodovia é
 *       superestimada.</li>
 *   <li><strong>Volume da carga.</strong> {@link CargoOccupancy} considera peso e volume,
 *       o que for mais restritivo. Produto não tem volume cadastrado, então só o peso
 *       entra. Carga leve e volumosa aparece como caminhão vazio.</li>
 * </ul>
 *
 * <p>Mudou a fórmula no {@link CostMatrixBuilder}? Mudou aqui também. São dois lugares
 * de propósito — o experimento precisa dela por aresta, o produto precisa por rota —,
 * mas a constante de desgaste é lida de lá, não copiada.
 */
@Component
public class RouteCostEstimator {

  private static final double LOAD_FUEL_FACTOR = 0.30;
  private static final double LOAD_WEAR_FACTOR = 0.50;

  /** Sem mistura de via conhecida, a referência: multiplicadores 1,0. */
  private static final RoadType REFERENCE_ROAD = RoadType.ARTERIAL;

  /**
   * Quanto custou rodar, aberto por componente.
   *
   * @param fuelLiters  litros estimados
   * @param fuelCost    R$ de combustível
   * @param tireCost    R$ de desgaste de pneu
   * @param driverCost  R$ de hora de motorista
   */
  public record CostBreakdown(double fuelLiters, double fuelCost, double tireCost, double driverCost) {

    public double total() {
      return fuelCost + tireCost + driverCost;
    }

    public CostBreakdown plus(CostBreakdown other) {
      return new CostBreakdown(
        fuelLiters + other.fuelLiters,
        fuelCost + other.fuelCost,
        tireCost + other.tireCost,
        driverCost + other.driverCost
      );
    }

    public static CostBreakdown zero() {
      return new CostBreakdown(0, 0, 0, 0);
    }
  }

  /**
   * @param distanceKm           distância total da rota
   * @param durationMin          duração total da rota
   * @param cargoWeightKg        peso da carga; zero quando não dá para somar (produto sem
   *                             peso cadastrado, ou rota antiga sem vínculo com pedidos)
   * @param profile              perfil de custo do veículo
   * @param consumptionLPer100Km consumo a usar — medido nos abastecimentos do veículo
   *                             quando houver, senão a constante do perfil
   * @param fuelPricePerLiter    preço a usar — o que a frota pagou, quando houver
   */
  public CostBreakdown estimate(
    double distanceKm,
    double durationMin,
    double cargoWeightKg,
    VehicleProfile profile,
    double consumptionLPer100Km,
    double fuelPricePerLiter
  ) {
    double loadFactor = CargoOccupancy.compute(cargoWeightKg, 0.0, profile).effectiveLoadFactor();

    double adjustedConsumption =
      consumptionLPer100Km * (1 + LOAD_FUEL_FACTOR * loadFactor) * REFERENCE_ROAD.fuelMultiplier;
    double liters = distanceKm * adjustedConsumption / 100.0;

    double wearFraction = CostMatrixBuilder.baseTireWearPerKm(profile)
      * (1 + LOAD_WEAR_FACTOR * loadFactor)
      * REFERENCE_ROAD.wearMultiplier;
    double tireCost = distanceKm * wearFraction * profile.tireReplacementCostPerTire();

    double driverCost = (durationMin / 60.0) * profile.driverCostPerHourReais();

    return new CostBreakdown(liters, liters * fuelPricePerLiter, tireCost, driverCost);
  }

  /**
   * Qual perfil de custo vale para um veículo, pela capacidade cadastrada.
   *
   * <p>O cadastro de veículo guarda capacidade em kg e mais nada do que o modelo precisa
   * — não há número de eixos, custo de pneu nem valor-hora de motorista. As faixas abaixo
   * mapeiam o veículo para um dos três perfis documentados do experimento. É aproximação
   * declarada: um 3/4 reforçado e um toco leve caem no mesmo perfil.
   */
  public VehicleProfile profileFor(Vehicle vehicle) {
    double capacityKg = vehicle == null ? 0.0 : vehicle.getCapacityKg();
    if (capacityKg <= 2_000) return VehicleProfile.light();
    if (capacityKg <= 8_000) return VehicleProfile.medium();
    return VehicleProfile.heavy();
  }
}
