package com.routewise.service;

import com.routewise.dto.PaymentMethodDto;
import com.routewise.dto.PaymentMethodRequestDto;
import com.routewise.entity.PaymentMethod;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentMethodServiceImpl implements IPaymentMethodService {

  private final PaymentMethodRepository repository;

  public PaymentMethodServiceImpl(PaymentMethodRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentMethodDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public PaymentMethodDto create(PaymentMethodRequestDto request) {
    PaymentMethod entity = new PaymentMethod(UUID.randomUUID().toString(), request.name());
    return toDto(repository.save(entity));
  }

  @Override
  public PaymentMethodDto update(String id, PaymentMethodRequestDto request) {
    PaymentMethod entity = findOrThrow(id);
    entity.setName(request.name());
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(String id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Forma de pagamento não encontrada: " + id);
    }
    repository.deleteById(id);
  }

  private PaymentMethod findOrThrow(String id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento não encontrada: " + id));
  }

  private PaymentMethodDto toDto(PaymentMethod entity) {
    return new PaymentMethodDto(entity.getId(), entity.getName());
  }
}
