package com.routewise.auth;

import java.util.UUID;

/**
 * Quem está usando o painel, lido do token.
 *
 * <p>Fica como atributo da requisição ({@link PanelAuthFilter#SESSION_ATTRIBUTE}) para
 * que um controller possa saber de quem é a ação sem reabrir o token.
 */
public record PanelSession(UUID userId, String email, String name) {}
