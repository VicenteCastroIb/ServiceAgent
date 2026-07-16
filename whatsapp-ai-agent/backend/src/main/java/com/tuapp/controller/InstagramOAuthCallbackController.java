package com.tuapp.controller;

import com.tuapp.service.InstagramOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * redirect_uri de la conexión self-service de Instagram (ver
 * InstagramOAuthService, doc sección 12) - Meta redirige acá el navegador del
 * dueño después de que autoriza (o rechaza) el acceso. Público a nivel de
 * Spring Security (/webhooks/** permitAll, ver SecurityConfig) porque es el
 * propio navegador del usuario el que llega, no puede mandar un JWT del
 * panel - la seguridad acá la da el "state" firmado (JwtService), no un
 * login de sesión.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/instagram/oauth")
public class InstagramOAuthCallbackController {

    private final InstagramOAuthService instagramOAuthService;
    private final String panelPublicUrl;

    public InstagramOAuthCallbackController(
            InstagramOAuthService instagramOAuthService,
            @Value("${panel.public-url:}") String panelPublicUrl) {
        this.instagramOAuthService = instagramOAuthService;
        this.panelPublicUrl = panelPublicUrl;
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null || code == null || state == null) {
            // El dueño canceló la autorización (error=access_denied) o Meta
            // mandó un redirect incompleto - no hay tenantId confiable acá
            // todavía (recién se valida al leer el state), así que no
            // podemos redirigir a una pantalla específica del negocio.
            log.info("Conexión de Instagram cancelada o incompleta (error={})", error);
            return respuestaFinal(null, false, "No se pudo conectar Instagram (¿cancelaste el permiso?).");
        }

        Long tenantId;
        try {
            tenantId = instagramOAuthService.leerTenantIdDeEstado(state);
        } catch (IllegalArgumentException e) {
            log.warn("State inválido en callback de Instagram OAuth: {}", e.getMessage());
            return respuestaFinal(null, false, e.getMessage());
        }

        try {
            instagramOAuthService.completarConexion(tenantId, code);
            return respuestaFinal(tenantId, true, null);
        } catch (Exception e) {
            log.error("Error completando la conexión de Instagram para tenant {}", tenantId, e);
            return respuestaFinal(tenantId, false, "No se pudo completar la conexión. Probá de nuevo.");
        }
    }

    private ResponseEntity<?> respuestaFinal(Long tenantId, boolean exito, String mensajeError) {
        if (panelPublicUrl != null && !panelPublicUrl.isBlank() && tenantId != null) {
            String destino = panelPublicUrl.replaceAll("/+$", "") + "/tenants/" + tenantId + "/edit"
                    + (exito ? "?instagram=conectado" : "?instagram=error");
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destino)).build();
        }
        // Sin panel.public-url configurada (o sin tenantId confiable si el
        // state falló) no hay a dónde redirigir con seguridad - se muestra
        // un mensaje simple en vez de un redirect a ningún lado.
        String texto = exito
                ? "<p>Listo, tu Instagram quedó conectado. Ya podés cerrar esta ventana.</p>"
                : "<p>" + (mensajeError != null ? mensajeError : "No se pudo conectar Instagram.") + "</p>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(texto);
    }
}
