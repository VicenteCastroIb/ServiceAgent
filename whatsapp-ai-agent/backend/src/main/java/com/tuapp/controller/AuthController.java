package com.tuapp.controller;

import com.tuapp.security.JwtService;
import com.tuapp.security.PanelUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login del panel (Next.js): valida usuario/clave y devuelve un JWT para que
 * el frontend lo mande en el header Authorization de cada request a
 * /admin/**.
 *
 * Valida contra PanelUserDetailsService: el admin (PANEL_USERNAME/PANEL_PASSWORD,
 * ve todos los negocios) o el login propio de un tenant (Tenant.panelUsername,
 * ve solo el suyo - ver TenantService.fijarCredencialesPanel). El JWT lleva
 * el tenantId embebido para que los controllers filtren el acceso.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication resultado;
        try {
            resultado = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Cubre tanto clave incorrecta como usuario inexistente - no hay
            // que distinguirlos en la respuesta (evita filtrar qué usuarios existen).
            return ResponseEntity.status(401).build();
        }

        Long tenantId = resultado.getPrincipal() instanceof PanelUserDetails principal
                ? principal.getTenantId()
                : null;
        String token = jwtService.generarToken(request.username(), tenantId);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token) {
    }
}
