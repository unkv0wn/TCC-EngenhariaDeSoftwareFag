package com.routewise.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a leitura dos escopos de um cliente de integração.
 *
 * <p>O que isto protege: a coluna é texto separado por vírgula, e decidir acesso lendo-a
 * crua é um erro que compila, passa despercebido e abre dado. {@code "orders:read"}
 * aparece dentro de {@code "orders:readonly"} numa busca por substring — um escopo que
 * ninguém concedeu passaria a valer.
 */
class ApiClientTest {

  private ApiClient withScopes(String scopes) {
    return new ApiClient(UUID.randomUUID(), "Teste", "rw_id_x", "hash", scopes, ApiClient.STATUS_ACTIVE);
  }

  @Test
  @DisplayName("separa os escopos da coluna, ignorando espaços")
  void parsesScopes() {
    assertEquals(
      Set.of("orders:read", "routes:read"),
      withScopes("orders:read, routes:read").getScopeSet()
    );
  }

  @Test
  @DisplayName("escopo parecido não vale pelo concedido")
  void similarScopeDoesNotMatch() {
    ApiClient client = withScopes("orders:readonly");

    // Comparação por substring diria que sim, e o cliente leria pedidos sem ter o escopo.
    assertFalse(client.hasScope("orders:read"));
    assertTrue(client.hasScope("orders:readonly"));
  }

  @Test
  @DisplayName("escopo que não foi concedido não vale")
  void missingScope() {
    ApiClient client = withScopes("orders:read");

    assertTrue(client.hasScope("orders:read"));
    assertFalse(client.hasScope("routes:read"));
    assertFalse(client.hasScope("deliveries:read"));
  }

  @Test
  @DisplayName("coluna vazia não concede nada")
  void emptyScopesGrantNothing() {
    assertTrue(withScopes("").getScopeSet().isEmpty());
    assertFalse(withScopes("").hasScope("orders:read"));
    assertTrue(withScopes("  ,  ").getScopeSet().isEmpty());
  }

  @Test
  @DisplayName("revogado não está ativo")
  void revokedIsNotActive() {
    ApiClient client = withScopes("orders:read");
    assertTrue(client.isActive());

    client.setStatus(ApiClient.STATUS_REVOKED);
    assertFalse(client.isActive());
  }
}
