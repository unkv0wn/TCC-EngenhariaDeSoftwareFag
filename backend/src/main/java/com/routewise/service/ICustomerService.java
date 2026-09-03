package com.routewise.service;

import com.routewise.dto.CustomerDto;
import com.routewise.dto.CustomerRequestDto;

import java.util.List;
import java.util.UUID;

public interface ICustomerService {

  List<CustomerDto> findAll();

  CustomerDto create(CustomerRequestDto request);

  CustomerDto update(UUID id, CustomerRequestDto request);

  void delete(UUID id);
}
