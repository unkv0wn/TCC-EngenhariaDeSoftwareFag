package com.routewise.service;

import com.routewise.dto.ProductDto;
import com.routewise.dto.ProductRequestDto;

import java.util.List;
import java.util.UUID;

public interface IProductService {

  List<ProductDto> findAll();

  ProductDto create(ProductRequestDto request);

  ProductDto update(UUID id, ProductRequestDto request);

  void delete(UUID id);
}
