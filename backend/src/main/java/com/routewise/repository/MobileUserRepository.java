package com.routewise.repository;

import com.routewise.entity.MobileUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MobileUserRepository extends JpaRepository<MobileUser, UUID> {

  Optional<MobileUser> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

  boolean existsByDriverId(UUID driverId);

  boolean existsByDriverIdAndIdNot(UUID driverId, UUID id);

  boolean existsByDeviceIdIgnoreCase(String deviceId);

  boolean existsByDeviceIdIgnoreCaseAndIdNot(String deviceId, UUID id);
}
