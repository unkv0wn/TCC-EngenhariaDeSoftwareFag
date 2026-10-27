package com.routewise.service;

import com.routewise.entity.Vehicle;
import com.routewise.service.RouteCostEstimator.CostBreakdown;
import com.routewise.validation.VehicleProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o custo estimado de uma rota.
 *
 * <p>O que isto protege: o número que o dashboard mostra é o único lugar do sistema em
 * que o modelo de custo do TCC aparece para quem usa. Errar um fator aqui não quebra
 * nada — a tela continua mostrando reais, com uma casa decimal, e ninguém tem como saber
 * de olho que o combustível virou metade do que deveria.
 *
 * <p>Os valores esperados são calculados à mão a partir da fórmula documentada no
 * {@code CostMatrixBuilder}, e não copiados de uma execução: teste que grava a saída de
 * hoje aprova o erro de amanhã junto.
 */
class RouteCostEstimatorTest {

  private final RouteCostEstimator estimator = new RouteCostEstimator();

  private Vehicle vehicleWithCapacity(double capacityKg) {
    return new Vehicle(
      UUID.randomUUID(), "ABC-1234", "Modelo", "Marca", 2020, "Branco", capacityKg, "diesel"
    );
  }

  @Test
  @DisplayName("caminhão vazio: consumo e desgaste sem acréscimo de carga")
  void emptyTruck() {
    VehicleProfile profile = VehicleProfile.medium();

    // 100 km, 2 h, sem carga, consumo 15 L/100km, diesel a R$ 6,00.
    CostBreakdown cost = estimator.estimate(100, 120, 0, profile, 15.0, 6.0);

    // litros = 100 * 15 / 100 = 15
    assertEquals(15.0, cost.fuelLiters(), 1e-9);
    assertEquals(90.0, cost.fuelCost(), 1e-9);
    // motorista = 2 h * R$ 35/h
    assertEquals(70.0, cost.driverCost(), 1e-9);
    assertTrue(cost.tireCost() > 0, "pneu desgasta mesmo vazio");
  }

  @Test
  @DisplayName("carga cheia acrescenta 30% ao consumo e 50% ao desgaste")
  void fullLoadRaisesFuelAndWear() {
    VehicleProfile profile = VehicleProfile.medium();

    CostBreakdown empty = estimator.estimate(100, 120, 0, profile, 15.0, 6.0);
    CostBreakdown full = estimator.estimate(100, 120, profile.capacityKg(), profile, 15.0, 6.0);

    assertEquals(empty.fuelLiters() * 1.30, full.fuelLiters(), 1e-9);
    assertEquals(empty.tireCost() * 1.50, full.tireCost(), 1e-9);
    // Hora de motorista não depende da carga: o caminhão leva o mesmo tempo.
    assertEquals(empty.driverCost(), full.driverCost(), 1e-9);
  }

  @Test
  @DisplayName("carga acima da capacidade não passa de 100% de ocupação")
  void overloadIsCapped() {
    VehicleProfile profile = VehicleProfile.medium();

    CostBreakdown full = estimator.estimate(100, 120, profile.capacityKg(), profile, 15.0, 6.0);
    CostBreakdown overloaded = estimator.estimate(100, 120, profile.capacityKg() * 3, profile, 15.0, 6.0);

    // Sem o teto, a fórmula seguiria subindo e um erro de cadastro de peso viraria um
    // custo três vezes maior sem nada na tela sugerindo o porquê.
    assertEquals(full.fuelLiters(), overloaded.fuelLiters(), 1e-9);
    assertEquals(full.tireCost(), overloaded.tireCost(), 1e-9);
  }

  @Test
  @DisplayName("rota de distância zero custa só a hora do motorista")
  void zeroDistance() {
    CostBreakdown cost = estimator.estimate(0, 60, 0, VehicleProfile.medium(), 15.0, 6.0);

    assertEquals(0.0, cost.fuelLiters(), 1e-9);
    assertEquals(0.0, cost.tireCost(), 1e-9);
    assertEquals(35.0, cost.driverCost(), 1e-9);
  }

  @Test
  @DisplayName("o consumo medido do veículo substitui a constante do perfil")
  void measuredConsumptionIsUsed() {
    VehicleProfile profile = VehicleProfile.medium();

    CostBreakdown byProfile = estimator.estimate(
      100, 60, 0, profile, profile.baseFuelConsumptionLPer100Km(), 6.0
    );
    CostBreakdown measured = estimator.estimate(100, 60, 0, profile, 9.0, 6.0);

    assertEquals(15.0, byProfile.fuelLiters(), 1e-9);
    assertEquals(9.0, measured.fuelLiters(), 1e-9);
  }

  @Test
  @DisplayName("o perfil sai da capacidade cadastrada do veículo")
  void profileComesFromCapacity() {
    assertEquals(VehicleProfile.light().label(), estimator.profileFor(vehicleWithCapacity(1_500)).label());
    assertEquals(VehicleProfile.medium().label(), estimator.profileFor(vehicleWithCapacity(4_000)).label());
    assertEquals(VehicleProfile.heavy().label(), estimator.profileFor(vehicleWithCapacity(12_000)).label());
  }

  @Test
  @DisplayName("veículo ausente não derruba a conta")
  void missingVehicleFallsBack() {
    // Rota cujo veículo foi excluído depois. Devolver perfil nenhum faria o dashboard
    // inteiro falhar por causa de uma linha.
    assertEquals(VehicleProfile.light().label(), estimator.profileFor(null).label());
  }

  @Test
  @DisplayName("somar duas rotas soma componente a componente")
  void breakdownsAddUp() {
    CostBreakdown a = estimator.estimate(100, 60, 0, VehicleProfile.medium(), 15.0, 6.0);
    CostBreakdown b = estimator.estimate(50, 30, 0, VehicleProfile.medium(), 15.0, 6.0);
    CostBreakdown sum = a.plus(b);

    assertEquals(a.fuelLiters() + b.fuelLiters(), sum.fuelLiters(), 1e-9);
    assertEquals(a.total() + b.total(), sum.total(), 1e-9);
    assertEquals(CostBreakdown.zero().plus(a).total(), a.total(), 1e-9);
  }
}
