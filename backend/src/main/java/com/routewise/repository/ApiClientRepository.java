package com.routewise.repository;

import com.routewise.entity.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

  Optional<ApiClient> findByClientId(String clientId);

  boolean existsByClientId(String clientId);

  List<ApiClient> findAllByOrderByCreatedAtDesc();
}
