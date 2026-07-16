package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.security.JwtService;
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

/**
 * Conexión SELF-SERVICE de Instagram (doc sección 12): a diferencia de
 * InstagramController.fijarCredenciales (admin carga el token a mano, sin
 * flujo OAuth propio - doc sección 11, versión v1 original), esto implementa
 * "Business Login for Instagram" de Meta para que el DUEÑO del negocio
 * conecte su propia cuenta con un click, sin que el admin intervenga.
 * <p>
 * Ver https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/business-login
 * Flujo (3 pasos, todos server-to-server salvo el paso 1 que es el navegador
 * del dueño):
 * 1) authorize: el navegador del dueño va a instagram.com/oauth/authorize y
 *    vuelve con un "code" de un solo uso (válido 1 hora).
 * 2) access_token: cambiamos ese code por un token CORTO (server-to-server).
 * 3) graph.instagram.com/access_token: cambiamos el token corto por uno
 *    LARGO (60 días) - mismo formato que ya usa
 *    InstagramMessagingService.refrescarToken para renovarlo después.
 * <p>
 * El parámetro "state" de la autorización lleva el tenantId, pero FIRMADO
 * (reutilizando JwtService, ver generarEstado/leerTenantIdDeEstado) - si no
 * se firmara, cualquiera podría alterar el state en el redirect y hacer que
 * las credenciales de SU cuenta de Instagram queden guardadas contra el
 * tenant de OTRO negocio.
 * <p>
 * Requiere que Vicente tenga una Meta App con el producto Instagram agregado
 * (developers.facebook.com) y cargue instagram.oauth.app-id/app-secret - son
 * credenciales de la Instagram API con Instagram Login, DISTINTAS de
 * meta.app-secret (que firma los webhooks entrantes, doc sección 11.5.1).
 * Mientras el Meta App no pase App Review, esto solo funciona con cuentas de
 * Instagram agregadas como "tester" en el dashboard de Meta - no todavía con
 * clientes reales al público.
 */
@Slf4j
@Service
public class InstagramOAuthService {

    private static final String AUTHORIZE_URL = "https://www.instagram.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String LONG_LIVED_TOKEN_URL = "https://graph.instagram.com/access_token";
    private static final String SUBJECT_ESTADO_OAUTH = "instagram-oauth-state";

    private final TenantService tenantService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public InstagramOAuthService(
            TenantService tenantService,
            JwtService jwtService,
            ObjectMapper objectMapper,
            @Value("${instagram.oauth.app-id:}") String appId,
            @Value("${instagram.oauth.app-secret:}") String appSecret,
            @Value("${app.base-url}") String appBaseUrl) {
        this.tenantService = tenantService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.redirectUri = appBaseUrl.replaceAll("/+$", "") + "/webhooks/instagram/oauth/callback";
    }

    public boolean isConfigurado() {
        return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
    }

    /** Arma la URL de autorización para redirigir al navegador del dueño (ver InstagramController). */
    public String generarUrlAutorizacion(Long tenantId) {
        if (!isConfigurado()) {
            throw new IllegalStateException(
                    "Todavía no se configuró la Meta App para conexión de Instagram (instagram.oauth.app-id/app-secret).");
        }
        String state = jwtService.generarToken(SUBJECT_ESTADO_OAUTH, tenantId);
        String scope = String.join(",",
                "instagram_business_basic",
                "instagram_business_manage_messages");

        return AUTHORIZE_URL
                + "?client_id=" + urlEncode(appId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state);
    }

    /**
     * Valida el state firmado y devuelve el tenantId que inició la conexión.
     *
     * @throws IllegalArgumentException si el state es inválido, venció, o no
     *                                   tiene un tenantId asociado.
     */
    public Long leerTenantIdDeEstado(String state) {
        if (state == null || !jwtService.esValido(state)) {
            throw new IllegalArgumentException("El link de conexión venció o no es válido. Probá de nuevo.");
        }
        Long tenantId = jwtService.extraerTenantId(state);
        if (tenantId == null) {
            throw new IllegalArgumentException("El link de conexión no tiene un negocio asociado.");
        }
        return tenantId;
    }

    /**
     * Completa la conexión (ver InstagramOAuthCallbackController): cambia el
     * code por un token corto, ese por uno largo, y guarda las credenciales
     * del tenant - mismo destino final que
     * TenantService.fijarCredencialesInstagram, que ya usa el admin para
     * cargarlas a mano.
     */
    public void completarConexion(Long tenantId, String code) {
        CortoResultado corto = intercambiarCodePorTokenCorto(code);
        LargoResultado largo = intercambiarPorTokenLargo(corto.accessToken());

        Instant vencimiento = Instant.now().plusSeconds(largo.expiresInSegundos());
        tenantService.fijarCredencialesInstagram(tenantId, corto.instagramAccountId(), largo.accessToken(), vencimiento);
        log.info("Instagram conectado self-service para tenant {} (cuenta {})", tenantId, corto.instagramAccountId());
    }

    private CortoResultado intercambiarCodePorTokenCorto(String code) {
        String body = "client_id=" + urlEncode(appId)
                + "&client_secret=" + urlEncode(appSecret)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&code=" + urlEncode(code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        JsonNode respuesta = enviar(request, "cambiar el código de Instagram por un token");
        // Instagram API with Instagram Login devuelve {"data":[{"access_token":..,"user_id":..,"permissions":..}]}
        JsonNode primero = respuesta.path("data").path(0);
        String accessToken = primero.path("access_token").asText("");
        String userId = primero.path("user_id").asText("");
        if (accessToken.isBlank() || userId.isBlank()) {
            log.warn("Instagram /oauth/access_token no devolvió access_token/user_id: {}", respuesta);
            throw new IllegalStateException("Instagram no devolvió los datos esperados. Probá de nuevo.");
        }
        return new CortoResultado(accessToken, userId);
    }

    private LargoResultado intercambiarPorTokenLargo(String accessTokenCorto) {
        String query = "grant_type=ig_exchange_token"
                + "&client_secret=" + urlEncode(appSecret)
                + "&access_token=" + urlEncode(accessTokenCorto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LONG_LIVED_TOKEN_URL + "?" + query))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        JsonNode respuesta = enviar(request, "obtener el token de larga duración de Instagram");
        String accessToken = respuesta.path("access_token").asText("");
        long expiresIn = respuesta.path("expires_in").asLong(0);
        if (accessToken.isBlank() || expiresIn <= 0) {
            log.warn("Instagram /access_token (long-lived) no devolvió access_token/expires_in: {}", respuesta);
            throw new IllegalStateException("Instagram no devolvió el token de larga duración. Probá de nuevo.");
        }
        return new LargoResultado(accessToken, expiresIn);
    }

    private JsonNode enviar(HttpRequest request, String accionParaError) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Instagram OAuth respondió {} al intentar {}: {}", response.statusCode(), accionParaError, response.body());
                throw new IllegalStateException("No se pudo " + accionParaError + " en este momento.");
            }
            return objectMapper.readTree(response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de conexión al intentar {}", accionParaError, e);
            throw new IllegalStateException("No se pudo " + accionParaError + " en este momento.");
        }
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private record CortoResultado(String accessToken, String instagramAccountId) {
    }

    private record LargoResultado(String accessToken, long expiresInSegundos) {
    }
}
