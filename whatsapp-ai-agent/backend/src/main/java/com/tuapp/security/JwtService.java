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
 */
@Component
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expirationMinutes = expirationMinutes;
    }

    /** @param tenantId null si es el admin (ve todos los negocios). */
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

    public String extraerUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Null si el token es del admin (sin tenant asociado). */
    public Long extraerTenantId(String token) {
        Object valor = parseClaims(token).get(CLAIM_TENANT_ID);
        return valor != null ? ((Number) valor).longValue() : null;
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
