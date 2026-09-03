package com.routewise.repository;

import com.routewise.entity.PaymentCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentConditionRepository extends JpaRepository<PaymentCondition, UUID> {
}
