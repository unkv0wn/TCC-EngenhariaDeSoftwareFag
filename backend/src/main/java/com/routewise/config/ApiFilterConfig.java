package com.routewise.config;

import com.routewise.auth.JwtService;
import com.routewise.auth.IntegrationAuthFilter;
import com.routewise.auth.MobileAuthFilter;
import com.routewise.auth.PanelAuthFilter;
import com.routewise.service.IntegrationAuditService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prende cada filtro de superfície ao seu prefixo, e a mais nada.
 *
 * <p>O registro é explícito — e não {@code @Component} — justamente por causa do recorte:
 * um filtro anotado valeria para todos os pedidos, e as três superfícies não aceitam a
 * mesma credencial. São três porque as regras são mesmo diferentes: o celular manda
 * token de motorista no cabeçalho, o integrador manda token de escopo, o navegador manda
 * cookie.
 *
 * <p>Note que {@code /api/mobile-users} e {@code /api/api-clients} não casam com
 * {@code /api/mobile/*} nem com {@code /api/integration/*}: o hífen faz parte do
 * segmento, então são outros caminhos — e são telas do gestor, cobertas pelo filtro do
 * painel.
 */
@Configuration
public class ApiFilterConfig {

  /**
   * O painel, por último em escrita e por primeiro em importância: é ele que fechou a
   * porta que faltava. O padrão é {@code /api/*} inteiro, e o próprio filtro é quem
   * descasca as duas superfícies com filtro próprio e o caminho do login.
   */
  @Bean
  public FilterRegistrationBean<PanelAuthFilter> panelAuthFilter(JwtService jwtService) {
    FilterRegistrationBean<PanelAuthFilter> registration =
      new FilterRegistrationBean<>(new PanelAuthFilter(jwtService));
    registration.addUrlPatterns("/api/*");
    return registration;
  }

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
   * é painel, e quem o cobre é o filtro de cima.
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
