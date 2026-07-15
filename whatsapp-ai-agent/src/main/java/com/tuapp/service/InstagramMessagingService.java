package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * NUEVO (doc secciones 3 y 5.1: Instagram vía Graph API directa de Meta, sin
 * pasar por Twilio - a diferencia de WhatsApp). Envía mensajes salientes con
 * el token propio de cada tenant (Business Login for Instagram:
 * https://graph.instagram.com/{ig-id}/messages) y mantiene ese token
 * vigente.
 *
 * A diferencia de PaymentService/CatalogSyncService (credenciales del propio
 * comercio con un tercero, sin relación con Meta), acá igual seguimos el
 * mismo patrón simple: el token de Instagram se obtiene fuera de esta app
 * (Meta App Dashboard / Graph API Explorer) y se carga manualmente desde el
 * panel (ver InstagramController) - no hay flujo OAuth propio en v1.
 */
@Slf4j
@Service
public class InstagramMessagingService {

    private final TenantService tenantService;
    private final ObjectMapper objectMapper;
    private final String graphApiBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public InstagramMessagingService(
            TenantService tenantService,
            ObjectMapper objectMapper,
            @Value("${instagram.graph-api-base-url}") String graphApiBaseUrl) {
        this.tenantService = tenantService;
        this.objectMapper = objectMapper;
        this.graphApiBaseUrl = graphApiBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Manda un mensaje de texto a un cliente de Instagram (Send API). Solo
     * funciona dentro de la ventana de 24hs desde el último mensaje del
     * cliente (restricción de Meta, sin equivalente a las plantillas
     * "utility" de WhatsApp) - por eso Instagram queda 100% reactivo, sin
     * recordatorios proactivos (ver doc sección 6, ReminderJob es solo
     * WhatsApp).
     *
     * @throws IllegalStateException si el tenant no tiene Instagram
     *                                 configurado o falla el envío.
     */
    public void enviarMensaje(Tenant tenant, String igsid, String texto) {
        if (!tenant.isInstagramConfigurado()) {
            throw new IllegalStateException("Tenant " + tenant.getId() + " no tiene Instagram configurado.");
        }
        try {
            Map<String, Object> body = Map.of(
                    "recipient", Map.of("id", igsid),
                    "message", Map.of("text", texto));
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphApiBaseUrl + "/" + tenant.getInstagramAccountId() + "/messages"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tenant.getInstagramAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Instagram Send API respondió {} para tenant {}: {}",
                        response.statusCode(), tenant.getId(), response.body());
                throw new IllegalStateException("Instagram Send API respondió " + response.statusCode());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el mensaje de Instagram", e);
        }
    }

    /**
     * Refresca el token de larga duración de un tenant antes de que venza
     * (ver InstagramTokenRefreshJob). Requisito de Meta: el token a refrescar
     * debe tener al menos 24hs de antigüedad. Los errores se loguean y se
     * tragan - un tenant con problemas de token no debe frenar el refresh de
     * los demás (mismo criterio que el resto de los jobs del proyecto).
     */
    public void refrescarToken(Tenant tenant) {
        if (!tenant.isInstagramConfigurado()) {
            return;
        }
        try {
            String query = "grant_type=ig_refresh_token&access_token=" + urlEncode(tenant.getInstagramAccessToken());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphApiBaseUrl + "/refresh_access_token?" + query))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Refresh de token de Instagram respondió {} para tenant {}: {}",
                        response.statusCode(), tenant.getId(), response.body());
                return;
            }

            JsonNode json = objectMapper.readTree(response.body());
            String nuevoToken = json.path("access_token").asText("");
            long expiraEnSegundos = json.path("expires_in").asLong(0);
            if (nuevoToken.isBlank() || expiraEnSegundos <= 0) {
                log.warn("Respuesta de refresh de Instagram sin access_token/expires_in para tenant {}: {}",
                        tenant.getId(), response.body());
                return;
            }

            Instant nuevoVencimiento = Instant.now().plusSeconds(expiraEnSegundos);
            tenantService.actualizarTokenInstagram(tenant, nuevoToken, nuevoVencimiento);
            log.info("Token de Instagram refrescado para tenant {} (vence {})", tenant.getId(), nuevoVencimiento);
        } catch (Exception e) {
            log.error("Error refrescando token de Instagram para tenant {}", tenant.getId(), e);
        }
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
