package com.routewise.auth;

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
import java.util.Optional;

/**
 * Exige sessão de motorista em {@code /api/mobile/**}.
 *
 * <p>Sem isto a tela de login seria enfeite: o celular passaria a pedir senha, mas os
 * endpoints continuariam aceitando qualquer chamada que soubesse o código da rota — e o
 * código circula por WhatsApp e QR impresso. É aqui que o cadastro de aparelho deixa de
 * ser uma regra do app e passa a ser uma regra do servidor.
 *
 * <p>É um filtro de servlet simples, e não Spring Security: entrar com o starter traria
 * uma cadeia que protege {@code /api/**} inteiro de uma vez, e o CRUD do painel hoje não
 * tem autenticação nenhuma para oferecer (ver {@code AuthController}). O recorte aqui é
 * exatamente a superfície do celular.
 *
 * <p>O que este filtro <strong>não</strong> faz: conferir se a rota pedida pertence ao
 * motorista do token. Hoje o acesso continua sendo pelo código da rota; o token prova que
 * é um motorista cadastrado, num aparelho cadastrado, não que seja o motorista daquela
 * rota. Amarrar as duas coisas é passo separado.
 */
public class MobileAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(MobileAuthFilter.class);

  /** Onde a sessão nasce — o único caminho que passa sem token. */
  private static final String LOGIN_PATH = "/api/mobile/auth/";

  private static final String BEARER_PREFIX = "Bearer ";

  /** Sessão do motorista para quem vier depois no pedido. */
  public static final String SESSION_ATTRIBUTE = "routewise.mobileSession";

  private final JwtService jwtService;

  public MobileAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // O preflight do CORS não carrega Authorization — barrá-lo faria o pedido real
    // nunca sair, e com erro que não aponta para cá.
    if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
    return request.getRequestURI().startsWith(LOGIN_PATH);
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  ) throws ServletException, IOException {
    Optional<MobileSession> session = bearerToken(request).flatMap(jwtService::parseMobileSession);

    if (session.isEmpty()) {
      log.warn("Mobile request without a valid session: {} {}", request.getMethod(), request.getRequestURI());
      writeUnauthorized(response);
      return;
    }

    request.setAttribute(SESSION_ATTRIBUTE, session.get());
    chain.doFilter(request, response);
  }

  private Optional<String> bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();

    String token = header.substring(BEARER_PREFIX.length()).trim();
    return token.isEmpty() ? Optional.empty() : Optional.of(token);
  }

  /**
   * Problem detail escrito à mão: filtro roda antes do {@code DispatcherServlet}, então
   * o {@code GlobalExceptionHandler} não vê o que acontece aqui. O formato é o mesmo
   * (RFC 9457) porque o app lê {@code detail} de toda resposta de erro.
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
