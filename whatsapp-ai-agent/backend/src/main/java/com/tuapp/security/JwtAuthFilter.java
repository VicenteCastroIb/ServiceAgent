package com.tuapp.security;

import com.tuapp.model.Tenant;
import com.tuapp.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica cada request buscando el JWT primero en el header
 * "Authorization: Bearer &lt;token&gt;" y, si no está, en la cookie httpOnly
 * que deja AuthController.login (ver JwtService.COOKIE_NAME). El panel
 * (Next.js, en otro origen/puerto) usa la cookie desde la migración a
 * httpOnly - el token ya no es legible por JS, así un XSS en el panel no
 * puede robarlo. El header sigue soportado para otros clientes (scripts,
 * integraciones) que sí manejen el token explícitamente. Sin sesión: la API
 * sigue siendo stateless (STATELESS en SecurityConfig), la cookie solo es el
 * transporte del mismo JWT de siempre.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TenantRepository tenantRepository;

    public JwtAuthFilter(JwtService jwtService, TenantRepository tenantRepository) {
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);
        if (token != null && jwtService.esValido(token) && tokenVigente(token)) {
            String username = jwtService.extraerUsername(token);
            Long tenantId = jwtService.extraerTenantId(token);
            var authentication = new UsernamePasswordAuthenticationToken(username, null, List.of());
            // tenantId viaja en "details": null = admin (ve todo), si no,
            // el dueño de ese negocio (ver PanelAuth, que lo lee acá).
            authentication.setDetails(tenantId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /** Header primero (compatibilidad con clientes no-browser), cookie httpOnly como fallback (panel web). */
    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (JwtService.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * true para tokens de admin (sin tenantId - firma+expiración ya alcanza,
     * ver esValido). Para un token de tenant, además exige que lleve el claim
     * tokenVersion y que coincida con el valor ACTUAL guardado en ese Tenant
     * (ver JwtService.generarTokenTenant y Tenant.tokenVersion) - así, un JWT
     * ya emitido deja de autenticar de inmediato después de un reset de
     * credenciales (TenantService.fijarCredencialesPanel) o de borrar el
     * tenant, sin esperar a que expire solo.
     * <p>
     * Un token CON tenantId pero SIN tokenVersion (ej. el "state" firmado por
     * InstagramOAuthService para el flujo OAuth, que reutiliza JwtService con
     * otro propósito) nunca queda vigente acá a propósito - no puede usarse
     * como si fuera un login real del panel aunque se filtrara.
     */
    private boolean tokenVigente(String token) {
        Long tenantId = jwtService.extraerTenantId(token);
        if (tenantId == null) {
            return true;
        }
        Integer tokenVersion = jwtService.extraerTokenVersion(token);
        if (tokenVersion == null) {
            return false;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::getTokenVersion)
                .map(actual -> actual.intValue() == tokenVersion.intValue())
                .orElse(false);
    }
}
