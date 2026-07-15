package com.tuapp.config;

import com.tuapp.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
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
 * Login: PanelUserDetailsService resuelve tanto al admin (ve todos los
 * tenants) como al dueño de cada negocio (ve solo el suyo, filtrado en cada
 * controller según el tenantId embebido en el JWT - ver JwtService/JwtAuthFilter).
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            @Value("${panel.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.allowedOrigins = allowedOrigins;
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
                        // Healthcheck de Railway/Render (ver application.properties, Actuator
                        // solo expone "health" - nada sensible queda público acá).
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // El UserDetailsService lo provee PanelUserDetailsService (admin + login
    // por tenant - ver com.tuapp.security). Al declarar un AuthenticationManager
    // propio, Spring Boot ya NO genera el usuario "user" con password aleatoria
    // de sus logs - ese mecanismo solo existe cuando no hay ningún
    // UserDetailsService/AuthenticationManager configurado.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Solo el/los origen(es) del panel Next.js pueden llamar a la API desde
     * el navegador. panel.allowed-origins acepta una lista separada por comas
     * (ver PANEL_ALLOWED_ORIGINS en .env/.env.example) - default
     * localhost:3000 para desarrollo local; en producción se carga la URL
     * real del panel desplegado (Railway/Render).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isBlank())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
