package com.routewise.service;

import com.routewise.dto.CompanySettingsDto;
import com.routewise.dto.CompanySettingsRequestDto;
import com.routewise.entity.CompanySettings;
import com.routewise.repository.CompanySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanySettingsServiceImpl implements ICompanySettingsService {

  private final CompanySettingsRepository repository;

  public CompanySettingsServiceImpl(CompanySettingsRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public CompanySettingsDto get() {
    return toDto(load());
  }

  @Override
  public CompanySettingsDto update(CompanySettingsRequestDto request) {
    CompanySettings entity = load();
    entity.setCompanyName(request.companyName());
    entity.setAddressZipCode(blankToNull(request.addressZipCode()));
    entity.setAddressStreet(blankToNull(request.addressStreet()));
    entity.setAddressNumber(blankToNull(request.addressNumber()));
    entity.setAddressDistrict(blankToNull(request.addressDistrict()));
    entity.setAddressCity(blankToNull(request.addressCity()));
    entity.setAddressState(blankToNull(request.addressState()));
    entity.setLatitude(request.latitude());
    entity.setLongitude(request.longitude());
    return toDto(repository.save(entity));
  }

  /** A linha única sempre existe (seed da V14); recria com defaults se sumir. */
  private CompanySettings load() {
    return repository.findById(CompanySettings.SINGLETON_ID).orElseGet(() -> {
      CompanySettings fresh = new CompanySettings(CompanySettings.SINGLETON_ID);
      fresh.setCompanyName("Minha Empresa");
      fresh.setAddressCity("São Paulo");
      fresh.setAddressState("SP");
      fresh.setLatitude(-23.5505);
      fresh.setLongitude(-46.6333);
      return repository.save(fresh);
    });
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private CompanySettingsDto toDto(CompanySettings entity) {
    return new CompanySettingsDto(
      entity.getCompanyName(),
      entity.getAddressZipCode(),
      entity.getAddressStreet(),
      entity.getAddressNumber(),
      entity.getAddressDistrict(),
      entity.getAddressCity(),
      entity.getAddressState(),
      entity.getLatitude(),
      entity.getLongitude()
    );
  }
}
