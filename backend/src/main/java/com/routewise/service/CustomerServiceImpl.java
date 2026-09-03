package com.routewise.service;

import com.routewise.dto.AddressDto;
import com.routewise.dto.CustomerDto;
import com.routewise.dto.CustomerRequestDto;
import com.routewise.entity.Customer;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements ICustomerService {

  private final CustomerRepository repository;

  public CustomerServiceImpl(CustomerRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CustomerDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public CustomerDto create(CustomerRequestDto request) {
    Customer entity = new Customer(UUID.randomUUID());
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public CustomerDto update(UUID id, CustomerRequestDto request) {
    Customer entity = findOrThrow(id);
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Cliente não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  private Customer findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
  }

  private void applyRequest(Customer entity, CustomerRequestDto request) {
    entity.setPersonType(request.personType());
    entity.setDocument(request.document());
    entity.setName(request.name());
    entity.setTradeName(request.tradeName());
    entity.setType(request.type());
    entity.setEmail(request.email());
    entity.setPhone(request.phone());

    AddressDto address = request.address();
    entity.setAddressZipCode(address.zipCode());
    entity.setAddressStreet(address.street());
    entity.setAddressNumber(address.number());
    entity.setAddressComplement(address.complement());
    entity.setAddressDistrict(address.district());
    entity.setAddressCity(address.city());
    entity.setAddressState(address.state());
  }

  private CustomerDto toDto(Customer entity) {
    AddressDto address = new AddressDto(
      entity.getAddressZipCode(),
      entity.getAddressStreet(),
      entity.getAddressNumber(),
      entity.getAddressComplement(),
      entity.getAddressDistrict(),
      entity.getAddressCity(),
      entity.getAddressState()
    );
    return new CustomerDto(
      entity.getId(),
      entity.getPersonType(),
      entity.getDocument(),
      entity.getName(),
      entity.getTradeName(),
      entity.getType(),
      entity.getEmail(),
      entity.getPhone(),
      address
    );
  }
}
