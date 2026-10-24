package com.routewise.config;

import com.routewise.auth.JwtService;
import com.routewise.auth.IntegrationAuthFilter;
import com.routewise.auth.MobileAuthFilter;
import com.routewise.service.IntegrationAuditService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prende cada filtro de superfície ao seu prefixo, e a mais nada.
 *
 * <p>O registro é explícito — e não {@code @Component} — justamente por causa do recorte:
 * um filtro anotado é aplicado a todos os pedidos, e aí o CRUD do painel, que não tem
 * autenticação para oferecer, pararia de responder.
 *
 * <p>Note que {@code /api/mobile-users} (o cadastro do gestor) não casa com o padrão
 * {@code /api/mobile/*}: o hífen faz parte do segmento, então é outro caminho.
 */
@Configuration
public class ApiFilterConfig {

  @Bean
  public FilterRegistrationBean<MobileAuthFilter> mobileAuthFilter(JwtService jwtService) {
    FilterRegistrationBean<MobileAuthFilter> registration =
      new FilterRegistrationBean<>(new MobileAuthFilter(jwtService));
    registration.addUrlPatterns("/api/mobile/*");
    return registration;
  }

  /**
   * Mesmo recorte, outra superfície: {@code /api/integration/*} e nada mais.
   *
   * <p>Note que {@code /api/api-clients} (o cadastro do gestor) não casa com o padrão —
   * é o painel, e segue aberto como o resto dele.
   */
  @Bean
  public FilterRegistrationBean<IntegrationAuthFilter> integrationAuthFilter(
    JwtService jwtService,
    IntegrationAuditService audit
  ) {
    FilterRegistrationBean<IntegrationAuthFilter> registration =
      new FilterRegistrationBean<>(new IntegrationAuthFilter(jwtService, audit));
    registration.addUrlPatterns("/api/integration/*");
    return registration;
  }
}
