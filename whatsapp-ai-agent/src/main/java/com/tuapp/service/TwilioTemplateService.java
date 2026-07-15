package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * NUEVO (doc sección 6: mensajes "utility" de recordatorio de cita). Crea y
 * manda a aprobación de WhatsApp las plantillas de mensaje vía la Content API
 * de Twilio (https://www.twilio.com/docs/content), en vez de cargarlas a mano
 * en el dashboard de Meta/Twilio.
 *
 * Importante: el proyecto usa UNA sola cuenta de Twilio para todos los
 * tenants (un único WABA - Twilio exige una relación 1:1 cuenta↔WABA, y las
 * plantillas se comparten entre todos los Senders/números de esa cuenta). Por
 * eso esto se hace UNA VEZ para toda la plataforma, no por negocio: el
 * contentSid resultante (configurado en reminders.content-sid) sirve para
 * recordarle citas a los clientes de cualquier tenant, sin volver a pedir
 * aprobación cuando se suma un negocio nuevo.
 *
 * Se implementa con HttpClient/JSON directo (no el SDK tipado de Twilio, que
 * también soporta esto) para poder verificar el request contra la doc de
 * Twilio de forma exacta y mantener el mismo patrón que PaymentService/
 * CatalogSyncService (Flow/WooCommerce) en vez de mezclar dos estilos.
 */
@Slf4j
@Service
public class TwilioTemplateService {

    private static final String CONTENT_API_BASE_URL = "https://content.twilio.com/v1/Content";

    private final String accountSid;
    private final String authToken;
    private final String idiomaPorDefecto;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TwilioTemplateService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${reminders.content-language:es}") String idiomaPorDefecto,
            ObjectMapper objectMapper) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.idiomaPorDefecto = idiomaPorDefecto;
        this.objectMapper = objectMapper;
    }

    public record PlantillaCreada(String contentSid, String estado) {
    }

    public record EstadoPlantilla(String estado, String motivoRechazo) {
    }

    /**
     * Crea una plantilla de contenido con un único bloque de texto (variables
     * tipo {{1}}, {{2}}, ...) y la manda a aprobación de WhatsApp en un solo
     * paso.
     *
     * @param friendlyName       nombre interno de la plantilla (no lo ve el cliente).
     * @param textoConVariables  el texto real, con placeholders {{1}}, {{2}}, ...
     * @param variablesEjemplo   valor de ejemplo por cada placeholder (clave =
     *                            número como string), que Meta pide para poder
     *                            revisar la plantilla.
     * @param categoria          "UTILITY", "MARKETING" o "AUTHENTICATION" (doc
     *                            sección 6: los recordatorios son "UTILITY").
     * @throws TwilioTemplateException si falla la creación o el envío a
     *                                   aprobación - mensaje pensado para
     *                                   mostrarle al admin.
     */
    public PlantillaCreada crearYEnviarAprobacion(
            String friendlyName, String textoConVariables, Map<String, String> variablesEjemplo, String categoria) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("friendly_name", friendlyName);
            body.put("language", idiomaPorDefecto);
            body.set("variables", objectMapper.valueToTree(variablesEjemplo));
            ObjectNode types = objectMapper.createObjectNode();
            ObjectNode textoBloque = objectMapper.createObjectNode();
            textoBloque.put("body", textoConVariables);
            types.set("twilio/text", textoBloque);
            body.set("types", types);

            HttpResponse<String> creacion = post(CONTENT_API_BASE_URL, body.toString());
            if (creacion.statusCode() / 100 != 2) {
                log.warn("Content API create respondió {}: {}", creacion.statusCode(), creacion.body());
                throw new TwilioTemplateException("No se pudo crear la plantilla en Twilio.");
            }
            String contentSid = objectMapper.readTree(creacion.body()).path("sid").asText("");
            if (contentSid.isBlank()) {
                throw new TwilioTemplateException("Twilio no devolvió el sid de la plantilla creada.");
            }

            ObjectNode aprobacion = objectMapper.createObjectNode();
            aprobacion.put("name", friendlyName);
            aprobacion.put("category", categoria);
            HttpResponse<String> envioAprobacion = post(
                    CONTENT_API_BASE_URL + "/" + contentSid + "/ApprovalRequests/whatsapp", aprobacion.toString());
            if (envioAprobacion.statusCode() / 100 != 2) {
                log.warn("Content API ApprovalRequests respondió {}: {}",
                        envioAprobacion.statusCode(), envioAprobacion.body());
                throw new TwilioTemplateException(
                        "La plantilla se creó (sid " + contentSid + ") pero no se pudo mandar a aprobación.");
            }
            String estado = objectMapper.readTree(envioAprobacion.body()).path("status").asText("received");

            log.info("Plantilla de WhatsApp creada y enviada a aprobación: contentSid={} estado={}", contentSid, estado);
            return new PlantillaCreada(contentSid, estado);
        } catch (TwilioTemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creando/enviando a aprobación la plantilla de WhatsApp", e);
            throw new TwilioTemplateException("No se pudo crear/enviar la plantilla a Twilio en este momento.");
        }
    }

    /**
     * Consulta el estado real de aprobación en WhatsApp de una plantilla ya
     * creada (received/pending/approved/rejected) - para saber cuándo copiar
     * el contentSid a reminders.content-sid sin tener que entrar a la consola
     * de Twilio.
     */
    public EstadoPlantilla consultarEstado(String contentSid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CONTENT_API_BASE_URL + "/" + contentSid + "/ApprovalRequests"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", basicAuth())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Content API ApprovalRequests (GET) respondió {}: {}", response.statusCode(), response.body());
                throw new TwilioTemplateException("No se pudo consultar el estado de la plantilla.");
            }
            JsonNode whatsapp = objectMapper.readTree(response.body()).path("whatsapp");
            String estado = whatsapp.path("status").asText("desconocido");
            String motivoRechazo = whatsapp.path("rejection_reason").asText("");
            return new EstadoPlantilla(estado, motivoRechazo);
        } catch (TwilioTemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error consultando estado de la plantilla {}", contentSid, e);
            throw new TwilioTemplateException("No se pudo consultar el estado de la plantilla en este momento.");
        }
    }

    private HttpResponse<String> post(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String basicAuth() {
        String credenciales = accountSid + ":" + authToken;
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}
