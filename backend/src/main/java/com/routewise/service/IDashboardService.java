package com.routewise.service;

import com.routewise.dto.DashboardDto;

public interface IDashboardService {

  /** Resumo da operação e do custo nos últimos {@code days} dias. */
  DashboardDto summary(int days);
}
