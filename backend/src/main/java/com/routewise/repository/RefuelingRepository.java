package com.routewise.repository;

import com.routewise.entity.Refueling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefuelingRepository extends JpaRepository<Refueling, UUID> {
}
