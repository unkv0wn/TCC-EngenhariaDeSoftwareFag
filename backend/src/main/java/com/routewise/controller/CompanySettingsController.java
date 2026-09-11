package com.routewise.controller;

import com.routewise.dto.CompanySettingsDto;
import com.routewise.dto.CompanySettingsRequestDto;
import com.routewise.service.ICompanySettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class CompanySettingsController {

  private final ICompanySettingsService service;

  public CompanySettingsController(ICompanySettingsService service) {
    this.service = service;
  }

  @GetMapping
  public CompanySettingsDto get() {
    return service.get();
  }

  @PutMapping
  public CompanySettingsDto update(@Valid @RequestBody CompanySettingsRequestDto request) {
    return service.update(request);
  }
}
