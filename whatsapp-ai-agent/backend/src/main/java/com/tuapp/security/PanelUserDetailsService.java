package com.tuapp.security;

import com.tuapp.model.Tenant;
import com.tuapp.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Resuelve tanto al admin (PANEL_USERNAME/PANEL_PASSWORD - vos, ver todos los
 * negocios) como a cualquier dueño de negocio (Tenant.panelUsername - solo ve
 * el suyo). Reemplaza el InMemoryUserDetailsManager de un único usuario fijo
 * que había antes de tener login por tenant.
 */
@Service
public class PanelUserDetailsService implements UserDetailsService {

    private final String adminUsername;
    private final String adminPasswordHash;
    private final TenantRepository tenantRepository;

    public PanelUserDetailsService(
            @Value("${panel.username}") String adminUsername,
            @Value("${panel.password}") String adminPassword,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder) {
        this.adminUsername = adminUsername;
        // Se hashea una sola vez al arrancar - la contraseña del admin no cambia en caliente.
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
        this.tenantRepository = tenantRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (adminUsername.equals(username)) {
            return new PanelUserDetails(username, adminPasswordHash, null, "ADMIN");
        }

        Tenant tenant = tenantRepository.findByPanelUsername(username)
                .filter(t -> t.getPanelPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new PanelUserDetails(username, tenant.getPanelPasswordHash(), tenant.getId(), "TENANT");
    }
}
