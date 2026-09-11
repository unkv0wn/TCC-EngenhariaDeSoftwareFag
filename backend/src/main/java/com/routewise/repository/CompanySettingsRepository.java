package com.routewise.repository;

import com.routewise.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, UUID> {
}
