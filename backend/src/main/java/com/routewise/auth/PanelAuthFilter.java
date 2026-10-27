package com.routewise.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Exige sessão de gestor no CRUD do painel.
 *
 * <p>Esta era a porta que faltava fechar. O app do motorista e a integração já exigiam
 * credencial; o painel — que é quem concede acesso ao app e cria credencial de
 * integração — respondia a qualquer um que alcançasse a porta 8080. A trava de aparelho
 * do celular e o escopo do integrador valiam só até alguém pedir direto ao backend.
 *
 * <p>Continua sendo filtro de servlet simples em vez do Spring Security, pelo mesmo
 * motivo dos outros dois: o starter traz uma cadeia única para {@code /api/**}, e aqui
 * há três superfícies com regras diferentes — o celular manda token no cabeçalho, o
 * integrador manda um token de outro tipo, e o painel manda um cookie. Separá-las em
 * filtros por prefixo é mais fácil de ler do que uma cadeia com três caminhos.
 *
 * <p><strong>Duas origens de token, de propósito.</strong> O navegador manda o cookie
 * {@code httpOnly}, que o JavaScript da página não consegue ler — é o que protege a
 * credencial de um XSS. Mas cliente de API (curl, Bruno, um script de teste) não tem
 * cookie para oferecer, e exigir um tornaria a superfície do painel impossível de testar
 * fora do navegador. O cabeçalho {@code Authorization} cobre esse caso.
 */
public class PanelAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(PanelAuthFilter.class);

  /** Onde a sessão nasce — o único caminho do painel que passa sem token. */
  private static final String LOGIN_PATH = "/api/auth/login";

  /**
   * Superfícies com filtro próprio.
   *
   * <p>Elas casam com o padrão {@code /api/*} deste filtro, então sem esta lista o
   * celular receberia 401 por não mandar cookie de painel — e com a mensagem errada.
   * Note que {@code /api/mobile-users} e {@code /api/api-clients} <em>não</em> estão
   * aqui: são telas do gestor, e é justamente nelas que a trava precisava chegar.
   */
  private static final List<String> OWN_FILTER_PREFIXES = List.of(
    "/api/mobile/",
    "/api/integration/"
  );

  private static final String BEARER_PREFIX = "Bearer ";

  /** Nome do cookie que o Next grava no login. Mudar aqui exige mudar lá. */
  public static final String COOKIE_NAME = "routewise";

  /** Sessão do gestor para quem vier depois no pedido. */
  public static final String SESSION_ATTRIBUTE = "routewise.panelSession";

  private final JwtService jwtService;

  public PanelAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // O preflight do CORS não carrega cookie nem Authorization — barrá-lo faria o pedido
    // real nunca sair, e com erro que não aponta para cá.
    if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

    String path = request.getRequestURI();
    if (LOGIN_PATH.equals(path)) return true;

    return OWN_FILTER_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  ) throws ServletException, IOException {
    Optional<PanelSession> session = token(request).flatMap(jwtService::parsePanelSession);

    if (session.isEmpty()) {
      log.warn("Panel request without a valid session: {} {}", request.getMethod(), request.getRequestURI());
      writeUnauthorized(response);
      return;
    }

    request.setAttribute(SESSION_ATTRIBUTE, session.get());
    chain.doFilter(request, response);
  }

  /** Cookie primeiro: é o caminho do navegador, que é de onde vem quase todo pedido. */
  private Optional<String> token(HttpServletRequest request) {
    Optional<String> fromCookie = cookieToken(request);
    return fromCookie.isPresent() ? fromCookie : headerToken(request);
  }

  private Optional<String> cookieToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return Optional.empty();

    return Arrays.stream(cookies)
      .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
      .map(Cookie::getValue)
      .filter(value -> value != null && !value.isBlank())
      .findFirst();
  }

  private Optional<String> headerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();

    String token = header.substring(BEARER_PREFIX.length()).trim();
    return token.isEmpty() ? Optional.empty() : Optional.of(token);
  }

  /**
   * Problem detail escrito à mão: filtro roda antes do {@code DispatcherServlet}, então
   * o {@code GlobalExceptionHandler} não vê o que acontece aqui. O formato é o mesmo
   * (RFC 9457) porque o `apiClient.ts` lê {@code detail} de toda resposta de erro.
   */
  private void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("""
      {"type":"about:blank","title":"Unauthorized","status":401,\
      "detail":"Sua sessão expirou. Entre novamente para continuar."}""");
  }
}
