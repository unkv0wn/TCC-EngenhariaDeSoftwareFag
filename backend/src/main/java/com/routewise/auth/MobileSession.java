package com.routewise.auth;

import java.util.UUID;

/**
 * Quem está falando, do lado do app, segundo o token.
 *
 * <p>O {@code deviceId} viaja dentro do token de propósito: o vínculo com o aparelho é
 * conferido no login, e sem ele aqui um token copiado para outro celular seguiria valendo
 * até expirar — o que esvaziaria justamente a garantia que o cadastro de aparelho dá.
 */
public record MobileSession(String username, UUID driverId, String deviceId) {}
