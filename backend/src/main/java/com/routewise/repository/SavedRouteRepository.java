package com.routewise.repository;

import com.routewise.entity.SavedRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedRouteRepository extends JpaRepository<SavedRoute, UUID> {

  List<SavedRoute> findAllByOrderByCreatedAtDesc();

  /** Entrada do app do motorista: a rota é encontrada pelo código, nunca pelo id. */
  Optional<SavedRoute> findByAccessCode(String accessCode);

  /** Rotas criadas a partir de um instante — a janela do dashboard. */
  List<SavedRoute> findByCreatedAtGreaterThanEqual(Instant from);

  /** As rotas de um motorista, para a tela inicial do app. */
  List<SavedRoute> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
}
