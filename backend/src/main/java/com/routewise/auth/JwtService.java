package com.routewise.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtService {

    /**
     * Separa o token do app do token do painel. Os dois saem da mesma chave, então sem
     * marca de origem um token do painel — que o backend emite para qualquer e-mail, sem
     * conferir senha — passaria pelo filtro do app como se fosse uma sessão de motorista.
     */
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_MOBILE = "mobile";
    private static final String CLAIM_DRIVER_ID = "driverId";
    private static final String CLAIM_DEVICE_ID = "deviceId";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${routewise.jwt.secret}") String secret,
            @Value("${routewise.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** Sessão do app do motorista, amarrada ao aparelho em que o login aconteceu. */
    public String generateMobileToken(String username, UUID driverId, String deviceId) {
        return generateToken(username, Map.of(
                CLAIM_TYPE, TYPE_MOBILE,
                CLAIM_DRIVER_ID, driverId.toString(),
                CLAIM_DEVICE_ID, deviceId
        ));
    }

    /**
     * Lê uma sessão de app de um token.
     *
     * <p>Devolve vazio em vez de lançar: assinatura errada, token expirado e token do
     * painel chegam todos aqui como "não é sessão de motorista", e quem chama responde
     * 401 do mesmo jeito nos três casos.
     */
    public Optional<MobileSession> parseMobileSession(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_MOBILE.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }

            return Optional.of(new MobileSession(
                    claims.getSubject(),
                    UUID.fromString(claims.get(CLAIM_DRIVER_ID, String.class)),
                    claims.get(CLAIM_DEVICE_ID, String.class)
            ));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}
