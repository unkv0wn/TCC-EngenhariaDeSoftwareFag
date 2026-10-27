package com.routewise.controller;

import com.routewise.dto.DashboardDto;
import com.routewise.service.DashboardServiceImpl;
import com.routewise.service.IDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A tela inicial do gestor. Leitura, e só. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final IDashboardService service;

  public DashboardController(IDashboardService service) {
    this.service = service;
  }

  @GetMapping
  public DashboardDto summary(
    @RequestParam(defaultValue = "" + DashboardServiceImpl.DEFAULT_DAYS) int days
  ) {
    return service.summary(days);
  }
}
