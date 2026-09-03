package com.routewise.service;

import com.routewise.dto.ProductDto;
import com.routewise.dto.ProductRequestDto;
import com.routewise.entity.Product;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductServiceImpl implements IProductService {

  private final ProductRepository repository;

  public ProductServiceImpl(ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public ProductDto create(ProductRequestDto request) {
    Product entity = new Product(UUID.randomUUID());
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public ProductDto update(UUID id, ProductRequestDto request) {
    Product entity = findOrThrow(id);
    applyRequest(entity, request);
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Produto não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  private void applyRequest(Product entity, ProductRequestDto request) {
    entity.setSku(request.sku());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setUnitId(request.unit());
    entity.setUnitPrice(request.unitPrice());
    entity.setWeightKg(request.weightKg());
  }

  private Product findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
  }

  private ProductDto toDto(Product entity) {
    return new ProductDto(
      entity.getId(),
      entity.getSku(),
      entity.getName(),
      entity.getDescription(),
      entity.getUnitId(),
      entity.getUnitPrice(),
      entity.getWeightKg()
    );
  }
}
