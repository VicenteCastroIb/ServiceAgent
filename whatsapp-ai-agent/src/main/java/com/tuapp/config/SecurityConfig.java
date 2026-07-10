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
 * CSRF se desactiva solo para esa ruta porque son POSTs servidor-a-servidor,
 * sin token CSRF disponible.
 *
 * Todo lo demás (futuro panel del dueño del local) exige autenticación.
 * TODO Semana 3: reemplazar el login in-memory generado por Spring Boot con
 * usuarios reales por tenant.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhooks/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
