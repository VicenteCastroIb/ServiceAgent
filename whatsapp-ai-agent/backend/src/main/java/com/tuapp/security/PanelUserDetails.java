package com.tuapp.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * UserDetails del panel (Next.js). tenantId es null para el admin (ve todos
 * los negocios); si no es null, identifica al dueño de ESE negocio - los
 * controllers /admin/** lo usan para restringir el acceso a su propio tenant
 * (ver PanelUserDetailsService y JwtAuthFilter, que lo propaga desde el JWT).
 */
public class PanelUserDetails implements UserDetails {

    private final String username;
    private final String passwordHash;
    private final Long tenantId;
    private final int tokenVersion;
    private final List<GrantedAuthority> authorities;

    public PanelUserDetails(String username, String passwordHash, Long tenantId, int tokenVersion, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.tenantId = tenantId;
        this.tokenVersion = tokenVersion;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Null si es el admin (ve todos los tenants). */
    public Long getTenantId() {
        return tenantId;
    }

    /** Sin uso para el admin (siempre 0) - ver Tenant.tokenVersion/JwtService.generarTokenTenant. */
    public int getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
