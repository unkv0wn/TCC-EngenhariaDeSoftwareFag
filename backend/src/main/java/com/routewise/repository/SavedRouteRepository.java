package com.routewise.repository;

import com.routewise.entity.SavedRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedRouteRepository extends JpaRepository<SavedRoute, UUID> {

  List<SavedRoute> findAllByOrderByCreatedAtDesc();
}
