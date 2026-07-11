package com.tuapp.controller;

import com.tuapp.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login del panel (Next.js): valida usuario/clave y devuelve un JWT para que
 * el frontend lo mande en el header Authorization de cada request a
 * /admin/**.
 *
 * Hoy valida contra el único usuario in-memory generado por Spring Boot
 * (usuario "user", password impresa en el log al arrancar) - suficiente
 * mientras hay un solo dueño/tester.
 * TODO: reemplazar por autenticación real por dueño de negocio (usuario/clave
 * por tenant).
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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
        String token = jwtService.generarToken(request.username());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token) {
    }
}
