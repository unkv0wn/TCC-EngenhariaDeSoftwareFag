package com.routewise.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa que um token de uma superfície não vale em outra.
 *
 * <p>O que isto protege: os três tokens — painel, app do motorista e integração — são
 * assinados com a <strong>mesma</strong> chave. A assinatura confere nos três, sempre.
 * O que os separa é uma única claim, e perder essa claim não quebra nada que apareça:
 * tudo continua compilando, o login continua funcionando, e um token do painel passa a
 * registrar entrega em nome de um motorista.
 *
 * <p>É o tipo de regressão que nenhuma tela mostra, por isso está fixada aqui.
 */
class JwtServiceTest {

  private static final String SECRET = "segredo-de-teste-com-tamanho-suficiente-para-hs512-0123456789-abcdefghij";

  private final JwtService jwt = new JwtService(SECRET, 60_000);

  private final UUID userId = UUID.randomUUID();
  private final UUID driverId = UUID.randomUUID();

  private String panelToken() {
    return jwt.generatePanelToken("gestor@routewise.com", userId, "Gestor");
  }

  private String mobileToken() {
    return jwt.generateMobileToken("carlos", driverId, "a1b2c3d4e5f6");
  }

  private String integrationToken() {
    return jwt.generateIntegrationToken("rw_id_teste", Set.of("orders:read"));
  }

  @Test
  @DisplayName("cada parser aceita o seu próprio token")
  void eachParserAcceptsItsOwn() {
    assertTrue(jwt.parsePanelSession(panelToken()).isPresent());
    assertTrue(jwt.parseMobileSession(mobileToken()).isPresent());
    assertTrue(jwt.parseIntegrationSession(integrationToken()).isPresent());
  }

  @Test
  @DisplayName("token do painel não vale como sessão de motorista nem de integração")
  void panelTokenIsRefusedElsewhere() {
    assertFalse(jwt.parseMobileSession(panelToken()).isPresent());
    assertFalse(jwt.parseIntegrationSession(panelToken()).isPresent());
  }

  @Test
  @DisplayName("token do motorista não vale no painel nem na integração")
  void mobileTokenIsRefusedElsewhere() {
    assertFalse(jwt.parsePanelSession(mobileToken()).isPresent());
    assertFalse(jwt.parseIntegrationSession(mobileToken()).isPresent());
  }

  @Test
  @DisplayName("token de integração não vale no painel nem no app")
  void integrationTokenIsRefusedElsewhere() {
    assertFalse(jwt.parsePanelSession(integrationToken()).isPresent());
    assertFalse(jwt.parseMobileSession(integrationToken()).isPresent());
  }

  @Test
  @DisplayName("token sem tipo não vale em lugar nenhum")
  void untypedTokenIsRefusedEverywhere() {
    // É o formato anterior do painel, emitido para qualquer e-mail sem conferir senha.
    // Aceitá-lo em qualquer parser manteria a porta aberta justamente para quem já
    // tivesse um guardado.
    String untyped = jwt.generateToken("qualquer@email.com");

    assertFalse(jwt.parsePanelSession(untyped).isPresent());
    assertFalse(jwt.parseMobileSession(untyped).isPresent());
    assertFalse(jwt.parseIntegrationSession(untyped).isPresent());
  }

  @Test
  @DisplayName("token assinado com outra chave não vale")
  void foreignSignatureIsRefused() {
    JwtService other = new JwtService("outra-chave-bem-diferente-mas-igualmente-longa-0123456789-abcdefghij", 60_000);

    assertFalse(jwt.parsePanelSession(other.generatePanelToken("x@y.com", userId, "X")).isPresent());
  }

  @Test
  @DisplayName("a sessão do painel carrega quem é")
  void panelSessionCarriesTheUser() {
    PanelSession session = jwt.parsePanelSession(panelToken()).orElseThrow();

    assertEquals(userId, session.userId());
    assertEquals("gestor@routewise.com", session.email());
    assertEquals("Gestor", session.name());
  }
}
