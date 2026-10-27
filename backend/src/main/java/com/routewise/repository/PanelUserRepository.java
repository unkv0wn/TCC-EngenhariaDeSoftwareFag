package com.routewise.repository;

import com.routewise.entity.PanelUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PanelUserRepository extends JpaRepository<PanelUser, UUID> {

  Optional<PanelUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

  /** Quantos acessos ativos existem além deste — ver a trava do último acesso no serviço. */
  long countByStatusAndIdNot(String status, UUID id);
}
