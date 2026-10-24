package com.routewise.repository;

import com.routewise.entity.IntegrationRequestLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegrationRequestLogRepository extends JpaRepository<IntegrationRequestLog, UUID> {

  /**
   * Os acessos mais recentes. Paginado por obrigação, não por capricho: é a única tabela
   * do projeto que cresce por requisição, e uma tela que pedisse tudo pararia de abrir
   * sozinha com o tempo.
   */
  List<IntegrationRequestLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
