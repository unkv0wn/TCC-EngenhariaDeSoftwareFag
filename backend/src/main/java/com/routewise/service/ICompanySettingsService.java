package com.routewise.service;

import com.routewise.dto.CompanySettingsDto;
import com.routewise.dto.CompanySettingsRequestDto;

public interface ICompanySettingsService {

  CompanySettingsDto get();

  CompanySettingsDto update(CompanySettingsRequestDto request);
}
