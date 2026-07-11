package com.tuapp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al tenant del usuario autenticado en el request actual (ver
 * JwtAuthFilter, que deja el tenantId en Authentication.details). Lo usan los
 * controllers /admin/** para restringir cada operación a su propio negocio.
 */
public final class PanelAuth {

    private PanelAuth() {
    }

    /** Null si el usuario autenticado es el admin (ve todos los negocios). */
    public static Long tenantIdActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object details = auth.getDetails();
        return details instanceof Long tenantId ? tenantId : null;
    }

    public static boolean esAdmin() {
        return tenantIdActual() == null;
    }

    /** true si es admin, o si el tenant autenticado coincide con tenantId. */
    public static boolean puedeAcceder(Long tenantId) {
        Long actual = tenantIdActual();
        return actual == null || actual.equals(tenantId);
    }
}
