package com.routewise.repository;

import com.routewise.entity.Refueling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RefuelingRepository extends JpaRepository<Refueling, UUID> {

  /**
   * Maior odômetro registrado por veículo, como pares {@code [vehicleId, odometerKm]}.
   *
   * <p>Agregado no banco em vez de carregar todos os abastecimentos para somar em
   * memória: a tela de veículos lista a frota inteira e isto cresce com o histórico.
   */
  @Query("select r.vehicleId, max(r.odometerKm) from Refueling r group by r.vehicleId")
  List<Object[]> findMaxOdometerByVehicle();
}
