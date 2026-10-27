package com.routewise.service;

import com.routewise.entity.Refueling;
import com.routewise.validation.VehicleProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a derivação do consumo a partir dos abastecimentos.
 *
 * <p>O que isto protege: {@code kmSincePrevious} não existe como coluna — a {@code V7}
 * deixou o campo fora de propósito, para que editar um registro antigo não deixasse os
 * seguintes desatualizados. A consequência é que a distância de cada tanque é derivada
 * em tempo de leitura, e um erro nessa derivação não quebra nada: a tela continua
 * mostrando km/L plausível, com uma casa decimal, e dali ele entra no custo de toda rota
 * daquele veículo.
 *
 * <p>Esse mesmo cálculo já produziu quilometragem negativa no front, por usar a maior
 * leitura do veículo como "anterior" em vez da imediatamente abaixo. Aqui ele é testado
 * com a lista fora de ordem de propósito.
 */
class RouteCostingServiceTest {

  /** Só os métodos de derivação são exercitados, então os colaboradores não são tocados. */
  private final RouteCostingService service =
    new RouteCostingService(null, null, null, null, null, new RouteCostEstimator());

  private final UUID vehicleId = UUID.randomUUID();

  private Refueling refueling(String date, double odometerKm, double liters, double price) {
    return new Refueling(
      UUID.randomUUID(), vehicleId, UUID.randomUUID(), LocalDate.parse(date), odometerKm, liters, price
    );
  }

  @Test
  @DisplayName("consumo sai da diferença de odômetro, não da ordem da lista")
  void consumptionIgnoresListOrder() {
    // 15.000 -> 15.650 = 650 km com 45 L. 650/45 = 14,44 km/L, ou 6,923 L/100km.
    List<Refueling> outOfOrder = List.of(
      refueling("2026-07-15", 15_650, 45, 6.10),
      refueling("2026-07-01", 15_000, 40, 6.10)
    );

    double consumption = service.measuredConsumption(outOfOrder, VehicleProfile.medium());

    assertEquals(45.0 / 650.0 * 100.0, consumption, 0.001);
  }

  @Test
  @DisplayName("odômetro que andou para trás é descartado, não dividido")
  void nonPositiveIntervalIsSkipped() {
    // O do meio repete a leitura: 0 km com 50 L daria consumo infinito.
    List<Refueling> withDuplicate = List.of(
      refueling("2026-07-01", 15_000, 40, 6.10),
      refueling("2026-07-10", 15_000, 50, 6.10),
      refueling("2026-07-15", 15_650, 45, 6.10)
    );

    double consumption = service.measuredConsumption(withDuplicate, VehicleProfile.medium());

    assertTrue(Double.isFinite(consumption), "consumo tem de ser finito");
    // Sobra o único trecho válido: 650 km com 45 L.
    assertEquals(45.0 / 650.0 * 100.0, consumption, 0.001);
  }

  @Test
  @DisplayName("um abastecimento sozinho não mede nada: cai na constante do perfil")
  void singleRefuelingFallsBackToProfile() {
    VehicleProfile profile = VehicleProfile.medium();

    double consumption = service.measuredConsumption(List.of(refueling("2026-07-01", 15_000, 40, 6.10)), profile);

    assertEquals(profile.baseFuelConsumptionLPer100Km(), consumption, 0.001);
  }
}
