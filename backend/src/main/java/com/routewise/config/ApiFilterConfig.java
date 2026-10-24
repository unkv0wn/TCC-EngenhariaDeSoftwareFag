package com.routewise.config;

import com.routewise.auth.JwtService;
import com.routewise.auth.MobileAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prende o {@link MobileAuthFilter} em {@code /api/mobile/*} e em mais nada.
 *
 * <p>O registro é explícito — e não {@code @Component} — justamente por causa do recorte:
 * um filtro anotado é aplicado a todos os pedidos, e aí o CRUD do painel, que não tem
 * autenticação para oferecer, pararia de responder.
 *
 * <p>Note que {@code /api/mobile-users} (o cadastro do gestor) não casa com o padrão
 * {@code /api/mobile/*}: o hífen faz parte do segmento, então é outro caminho.
 */
@Configuration
public class MobileAuthFilterConfig {

  @Bean
  public FilterRegistrationBean<MobileAuthFilter> mobileAuthFilter(JwtService jwtService) {
    FilterRegistrationBean<MobileAuthFilter> registration =
      new FilterRegistrationBean<>(new MobileAuthFilter(jwtService));
    registration.addUrlPatterns("/api/mobile/*");
    return registration;
  }
}
