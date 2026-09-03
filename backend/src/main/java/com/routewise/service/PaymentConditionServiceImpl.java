package com.routewise.service;

import com.routewise.dto.PaymentConditionDto;
import com.routewise.dto.PaymentConditionRequestDto;
import com.routewise.entity.PaymentCondition;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.PaymentConditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentConditionServiceImpl implements IPaymentConditionService {

  private final PaymentConditionRepository repository;

  public PaymentConditionServiceImpl(PaymentConditionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentConditionDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public PaymentConditionDto create(PaymentConditionRequestDto request) {
    PaymentCondition entity = new PaymentCondition(UUID.randomUUID());
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public PaymentConditionDto update(UUID id, PaymentConditionRequestDto request) {
    PaymentCondition entity = findOrThrow(id);
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Condição de pagamento não encontrada: " + id);
    }
    repository.deleteById(id);
  }

  private void applyRequest(PaymentCondition entity, PaymentConditionRequestDto request) {
    entity.setName(request.name());
    entity.setInstallments(request.installments());
    entity.setIntervalDays(request.intervalDays());
  }

  private PaymentCondition findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Condição de pagamento não encontrada: " + id));
  }

  private PaymentConditionDto toDto(PaymentCondition entity) {
    return new PaymentConditionDto(entity.getId(), entity.getName(), entity.getInstallments(), entity.getIntervalDays());
  }
}
