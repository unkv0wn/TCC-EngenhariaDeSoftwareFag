package com.routewise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client configuration.
 *
 * <p>Provides a {@link RestTemplate} bean with explicit connection and read
 * timeouts so that a slow or unreachable OSRM server never blocks a virtual
 * thread indefinitely.
 */
@Configuration
public class RestTemplateConfig {

  @Value("${routewise.osrm.timeout-ms:10000}")
  private int timeoutMs;

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5_000);   // 5 s connection timeout
    factory.setReadTimeout(timeoutMs);  // configurable read timeout
    return new RestTemplate(factory);
  }
}
