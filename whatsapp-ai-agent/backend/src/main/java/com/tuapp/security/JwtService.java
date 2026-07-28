package com.tuapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y valida los JWT que usa el panel (Next.js, en otro origen/puerto)
 * para autenticar sus requests a /admin/** en vez de sesión de cookies.
 *
 * Lleva el tenantId como claim (ver PanelUserDetails): null para el admin
 * (ve todos los negocios), o el id del negocio si es el login de un dueño -
 * los controllers /admin/** lo usan para restringir el acceso a lo suyo.
 *
 * Los tokens de tenant además llevan tokenVersion (ver generarTokenTenant),
 * que JwtAuthFilter revalida contra Tenant.tokenVersion en cada request -
 * eso permite invalidar un JWT ya emitido (reset de credenciales, borrado del
 * tenant) sin esperar a que expire solo. Los tokens de admin no tienen
 * tokenVersion: no hay una fila en base contra la cual revalidarlo (las
 * credenciales del admin son fijas por variables de entorno).
 */
@Component
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    /**
     * Nombre de la cookie httpOnly donde AuthController deja el JWT del panel
     * (ver AuthController.login/logout y JwtAuthFilter, que la lee acá mismo
     * para no duplicar el literal en tres archivos distintos).
     */
    public static final String COOKIE_NAME = "panel_token";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Token "simple", sin tokenVersion. Lo sigue usando el login del admin
     * (ver AuthController), y también InstagramOAuthService para firmar el
     * parámetro "state" del flujo OAuth de Instagram (no es un login real,
     * solo necesita ir firmado para no poder alterarse en el redirect de
     * Meta - ver su Javadoc). A propósito, un token de este método CON
     * tenantId nunca autentica en JwtAuthFilter: ese filtro exige el claim
     * tokenVersion para cualquier token que traiga tenantId, y este método no
     * lo agrega - así el "state" de Instagram no puede reusarse como si fuera
     * un login real del panel aunque se filtrara.
     *
     * @param tenantId null si es el admin (ve todos los negocios).
     */
    public String generarToken(String username, Long tenantId) {
        Instant ahora = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expirationMinutes * 60)));
        if (tenantId != null) {
            builder.claim(CLAIM_TENANT_ID, tenantId);
        }
        return builder.signWith(key).compact();
    }

    /**
     * Token de login real del panel para el DUEÑO de un negocio (ver
     * AuthController/PanelUserDetails) - lleva tokenVersion además de
     * tenantId, para poder revocarlo antes de que expire solo (ver Javadoc de
     * la clase y de Tenant.tokenVersion).
     */
    public String generarTokenTenant(String username, long tenantId, int tokenVersion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expirationMinutes * 60)))
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .signWith(key)
                .compact();
    }

    public String extraerUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Null si el token es del admin (sin tenant asociado). */
    public Long extraerTenantId(String token) {
        Object valor = parseClaims(token).get(CLAIM_TENANT_ID);
        return valor != null ? ((Number) valor).longValue() : null;
    }

    /** Null si el token no lleva ese claim (token de admin, o el "state" de InstagramOAuthService). */
    public Integer extraerTokenVersion(String token) {
        Object valor = parseClaims(token).get(CLAIM_TOKEN_VERSION);
        return valor != null ? ((Number) valor).intValue() : null;
    }

    /** Minutos de vigencia de un token - lo usa AuthController para el maxAge de la cookie. */
    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public boolean esValido(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
