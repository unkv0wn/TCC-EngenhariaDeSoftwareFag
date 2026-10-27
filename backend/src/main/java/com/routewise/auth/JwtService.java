package com.routewise.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtService {

    /**
     * Separa as três superfícies. Os três tokens saem da mesma chave, então sem marca de
     * origem um valeria pelo outro — e a conta não fecha em nenhuma direção: o token do
     * painel não deveria registrar entrega em nome de um motorista, e o da integração,
     * que é só de leitura, não deveria abrir o CRUD do gestor.
     */
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_MOBILE = "mobile";
    private static final String TYPE_INTEGRATION = "integration";
    private static final String TYPE_PANEL = "panel";
    private static final String CLAIM_DRIVER_ID = "driverId";
    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String CLAIM_SCOPES = "scopes";
    private static final String CLAIM_API_CLIENT_ID = "apiClientId";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_NAME = "name";

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

    /**
     * Sessão do painel, emitida depois de conferir a senha em {@code panel_users}.
     *
     * <p>Antes desta versão o token do painel não levava marca nenhuma, e era emitido
     * para qualquer e-mail sem conferir senha. Token antigo não passa mais: falta-lhe a
     * claim, e quem estiver com o cookie de ontem cai na tela de login. É o efeito
     * esperado de passar a exigir credencial, não um defeito.
     */
    public String generatePanelToken(String email, UUID userId, String name) {
        return generateToken(email, Map.of(
                CLAIM_TYPE, TYPE_PANEL,
                CLAIM_USER_ID, userId.toString(),
                CLAIM_NAME, name
        ));
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
     * Token de um sistema integrado, com os escopos que ele recebeu.
     *
     * <p>Os escopos vão dentro do token, e não são relidos do banco a cada requisição: é
     * o que torna a checagem de escopo barata. O preço é que revogar um escopo só vale
     * para o próximo token — por isso a validade é curta.
     */
    public String generateIntegrationToken(String clientId, UUID apiClientId, Set<String> scopes) {
        return generateToken(clientId, Map.of(
                CLAIM_TYPE, TYPE_INTEGRATION,
                CLAIM_API_CLIENT_ID, apiClientId.toString(),
                CLAIM_SCOPES, String.join(",", scopes)
        ));
    }

    /**
     * Lê uma sessão de painel de um token.
     *
     * <p>Vazio para token de motorista, token de integração e token sem tipo — este
     * último é o formato anterior, emitido sem conferir senha; aceitá-lo manteria a
     * porta aberta justamente para quem já tivesse um.
     */
    public Optional<PanelSession> parsePanelSession(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_PANEL.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }

            return Optional.of(new PanelSession(
                    UUID.fromString(claims.get(CLAIM_USER_ID, String.class)),
                    claims.getSubject(),
                    claims.get(CLAIM_NAME, String.class)
            ));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Lê uma sessão de integração de um token.
     *
     * <p>Vazio para qualquer token que não seja de integração — inclusive o do motorista
     * e o do painel. Os três saem da mesma chave, e sem a marca de tipo um valeria pelo
     * outro.
     */
    public Optional<IntegrationSession> parseIntegrationSession(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_INTEGRATION.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }

            String raw = claims.get(CLAIM_SCOPES, String.class);
            Set<String> scopes = raw == null || raw.isBlank()
                    ? Set.of()
                    : new LinkedHashSet<>(Arrays.stream(raw.split(",")).map(String::trim).toList());

            // O id interno entrou depois do resto. Um token da hora anterior não o tem, e
            // recusá-lo por isso derrubaria integrações no ar para resolver um campo de log.
            String rawId = claims.get(CLAIM_API_CLIENT_ID, String.class);
            UUID apiClientId = rawId == null ? null : UUID.fromString(rawId);

            return Optional.of(new IntegrationSession(claims.getSubject(), apiClientId, scopes));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
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
