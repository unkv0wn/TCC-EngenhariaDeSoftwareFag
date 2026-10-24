package com.routewise.auth;

import com.routewise.entity.IntegrationRequestLog;
import com.routewise.service.IntegrationAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Exige token de integração em {@code /api/integration/**} e confere o escopo do caminho.
 *
 * <p>Mesmo desenho do {@link MobileAuthFilter}: filtro de servlet simples, fora do Spring
 * Security, recortado a um prefixo. Entrar com o starter de segurança protegeria
 * {@code /api/**} inteiro de uma vez, e o CRUD do gestor ainda não tem autenticação para
 * oferecer.
 *
 * <p>O escopo é decidido aqui, e não em cada controller, porque é uma regra do caminho e
 * não da regra de negócio: espalhá-la pelos métodos faria cada endpoint novo depender de
 * alguém lembrar de anotá-lo, e esquecer significaria um endpoint sem escopo nenhum.
 */
public class IntegrationAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(IntegrationAuthFilter.class);

  /** Onde o token nasce — o único caminho que passa sem token. */
  private static final String TOKEN_PATH = "/api/integration/token";

  private static final String BEARER_PREFIX = "Bearer ";

  public static final String SESSION_ATTRIBUTE = "routewise.integrationSession";

  /**
   * Escopo exigido por prefixo de caminho.
   *
   * <p>Caminho que não casar com nenhuma entrada é recusado — negar por omissão, e não
   * liberar por omissão. Um endpoint novo que alguém esqueça de mapear aqui responde 403
   * até ser mapeado, o que é um defeito visível; o contrário seria um dado aberto em
   * silêncio.
   */
  private static final Map<String, String> SCOPE_BY_PREFIX = Map.of(
    "/api/integration/orders", "orders:read",
    "/api/integration/routes", "routes:read",
    "/api/integration/deliveries", "deliveries:read"
  );

  private final JwtService jwtService;
  private final IntegrationAuditService audit;

  public IntegrationAuthFilter(JwtService jwtService, IntegrationAuditService audit) {
    this.jwtService = jwtService;
    this.audit = audit;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // O preflight do CORS não carrega Authorization — barrá-lo faria o pedido real nunca
    // sair, com erro que não aponta para cá.
    if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
    return request.getRequestURI().startsWith(TOKEN_PATH);
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  ) throws ServletException, IOException {
    String path = request.getRequestURI();
    String method = request.getMethod();

    Optional<IntegrationSession> session = bearerToken(request).flatMap(jwtService::parseIntegrationSession);

    if (session.isEmpty()) {
      audit.record(null, null, method, path, IntegrationRequestLog.OUTCOME_BAD_TOKEN, 401);
      log.warn("Integration request without a valid token: {} {}", method, path);
      write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
        "Token ausente ou inválido. Obtenha um em POST /api/integration/token.");
      return;
    }

    IntegrationSession current = session.get();
    String required = requiredScope(path);

    if (required == null || !current.hasScope(required)) {
      audit.record(null, current.clientId(), method, path,
        IntegrationRequestLog.OUTCOME_MISSING_SCOPE, 403);
      log.warn("Integration request without scope {}: {} {}", required, method, path);
      write(response, HttpStatus.FORBIDDEN, "Insufficient Scope",
        required == null
          ? "Endereço não disponível para integração."
          : "Esta integração não tem o escopo '" + required + "'. Peça ao gestor.");
      return;
    }

    audit.record(null, current.clientId(), method, path, IntegrationRequestLog.OUTCOME_SUCCESS, 200);
    request.setAttribute(SESSION_ATTRIBUTE, current);
    chain.doFilter(request, response);
  }

  private String requiredScope(String path) {
    return SCOPE_BY_PREFIX.entrySet().stream()
      .filter(entry -> path.startsWith(entry.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  private Optional<String> bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();

    String token = header.substring(BEARER_PREFIX.length()).trim();
    return token.isEmpty() ? Optional.empty() : Optional.of(token);
  }

  /**
   * Problem detail escrito à mão: filtro roda antes do {@code DispatcherServlet}, então o
   * {@code GlobalExceptionHandler} não vê o que acontece aqui. O formato é o mesmo
   * (RFC 9457) que o resto da API usa.
   */
  private void write(HttpServletResponse response, HttpStatus status, String title, String detail)
    throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(
      "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status.value()
        + ",\"detail\":\"" + detail + "\"}"
    );
  }
}
