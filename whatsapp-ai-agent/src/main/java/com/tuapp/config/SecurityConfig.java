package com.tuapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del panel web (login del dueño del local) y de los endpoints de
 * webhook (validación de firma de Twilio/Meta).
 *
 * Los webhooks (/webhooks/**) quedan públicos a nivel de Spring Security:
 * su autenticación real es la validación de firma que hace WebhookController
 * contra cada request (RequestValidator con el auth token de Twilio), no un
 * login de sesión - Twilio no puede completar un login interactivo.
 *
 * /admin/** (API de administración, Semana 3) SÍ requiere autenticación
 * (login básico in-memory por ahora), pero se ignora CSRF ahí también:
 * es una API JSON pensada para curl/Postman, no un formulario de navegador
 * con sesión de cookies - CSRF protege ese segundo caso, no este.
 * Cuando exista el panel real con formularios (Thymeleaf), esas rutas SÍ
 * deben mantener CSRF activo.
 *
 * TODO Semana 3: reemplazar el login in-memory generado por Spring Boot con
 * usuarios reales por tenant.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/**", "/admin/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhooks/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
