package com.routewise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 *
 * <p>Allows the Next.js dev server (localhost:3000), the older Vite prototype
 * (localhost:5173), and any configured production origin to consume the REST
 * + SSE endpoints.
 */
@Configuration
public class CorsConfig {

  @Value("${routewise.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
  private String[] allowedOrigins;

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
          .allowedOrigins(allowedOrigins != null ? allowedOrigins : new String[0])
          .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
          .allowedHeaders("*")
          .exposedHeaders("Content-Type", "X-Request-ID")
          // Preciso para o cookie de sessão do painel: sem isto o navegador não manda o
          // `routewise` numa chamada para outra porta, e todo o CRUD responderia 401.
          // É o que obriga as origens a serem explícitas — com credenciais, o curinga
          // `*` é recusado pelo próprio navegador, não só por política nossa.
          .allowCredentials(true)
          .maxAge(3600);
      }
    };
  }
}
