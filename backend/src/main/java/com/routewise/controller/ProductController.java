package com.routewise.controller;

import com.routewise.dto.ProductDto;
import com.routewise.dto.ProductRequestDto;
import com.routewise.service.IProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final IProductService service;

  public ProductController(IProductService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProductDto> findAll() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductDto create(@Valid @RequestBody ProductRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public ProductDto update(@PathVariable UUID id, @Valid @RequestBody ProductRequestDto request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
