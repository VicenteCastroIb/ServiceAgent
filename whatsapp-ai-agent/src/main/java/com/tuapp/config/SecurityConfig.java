package com.tuapp.config;

import com.tuapp.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad de la API: los webhooks (Twilio/Meta) y /admin/** (consumido por
 * el panel Next.js, en otro origen/puerto).
 *
 * Los webhooks (/webhooks/**) quedan públicos a nivel de Spring Security: su
 * autenticación real es la validación de firma que hace WebhookController
 * contra cada request (RequestValidator con el auth token de Twilio), no un
 * login de sesión - Twilio no puede completar un login interactivo.
 *
 * /admin/** requiere un JWT válido (header "Authorization: Bearer &lt;token&gt;",
 * emitido por AuthController tras /auth/login). La API es 100% stateless (sin
 * sesión de cookies): por eso CSRF está deshabilitado por completo - CSRF
 * protege sesiones de cookies de navegador, no tokens Bearer - y se habilita
 * CORS solo para el origen del panel.
 *
 * TODO: reemplazar el usuario in-memory generado por Spring Boot (que valida
 * AuthController) por usuarios reales por tenant.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Solo el origen del panel Next.js puede llamar a la API desde el
     * navegador. TODO: agregar el dominio real de producción cuando exista.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
