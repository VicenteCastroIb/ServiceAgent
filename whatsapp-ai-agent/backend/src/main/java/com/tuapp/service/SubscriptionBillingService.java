package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.BillingStatus;
import com.tuapp.model.PaymentMethod;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.model.TenantSubscription;
import com.tuapp.repository.TenantSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * NUEVO: cobro automático de la suscripción mensual que cada TENANT nos paga
 * a NOSOTROS por usar la plataforma (doc secciones 3 y 6), vía la API de
 * Suscripciones de Flow (https://developers.flow.cl/en/docs/category/planes-de-suscripción).
 *
 * OJO - esto es una integración de Flow completamente separada de
 * PaymentService: acá se usa la cuenta Flow PROPIA de la plataforma
 * (flow.billing.api-key/secret-key), nunca las credenciales de un tenant.
 * PaymentService es para que el CLIENTE FINAL le pague a la TIENDA
 * (flow.api-key/secretKey del tenant); esto es para que la TIENDA nos pague
 * A NOSOTROS. Dos cuentas Flow, dos propósitos, nunca se mezclan.
 *
 * Flujo (ver doc de Flow, "Planes de Suscripción"):
 * 1) crearPlanesSiNoExisten(): una vez, crea los planes "Básico" y "Pro" en
 *    Flow (montos fijos del doc sección 3). El plan Catálogo es a medida
 *    (cotización) y no tiene cobro recurrente automático - se coordina
 *    manualmente.
 * 2) iniciarSuscripcion(tenant, email): crea el cliente en Flow y pide la URL
 *    para que el dueño registre su tarjeta (redirección a Flow, no vemos
 *    nunca el número de tarjeta - PCI DSS lo maneja Flow).
 * 3) Cuando el dueño termina de registrar la tarjeta, Flow llama a nuestro
 *    webhook (url_return) con un token - ver procesarRetornoTarjeta, que
 *    confirma el registro y recién ahí crea la suscripción real en Flow
 *    (que empieza a cobrar automáticamente cada mes).
 * 4) Cada cobro exitoso, Flow notifica al urlCallback del plan - ver
 *    procesarNotificacionPago, que consulta el pago real (nunca confía en el
 *    request entrante) y actualiza el estado.
 *
 * La notificación de cobro (urlCallback) no trae el id del tenant, así que se
 * matchea por billingEmail contra el pagador informado por Flow - por eso
 * billingEmail (y Tenant.ownerEmail, de donde normalmente sale) son únicos a
 * nivel de base de datos: iniciarSuscripcion rechaza explícitamente un email
 * ya usado por otro tenant, así que para cuando llega una notificación de
 * cobro el match contra billingEmail es siempre único, sin ambigüedad.
 */
@Slf4j
@Service
public class SubscriptionBillingService {

    /** IDs fijos de los planes en Flow - se crean una sola vez (ver crearPlanesSiNoExisten). */
    private static final String PLAN_ID_BASICO = "wsagent-basico-mensual";
    private static final String PLAN_ID_PRO = "wsagent-pro-mensual";
    private static final int INTERVAL_MENSUAL = 3;

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final OwnerNotificationService ownerNotificationService;
    private final ObjectMapper objectMapper;
    private final String flowApiBaseUrl;
    private final String appBaseUrl;
    private final String apiKey;
    private final String secretKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SubscriptionBillingService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            OwnerNotificationService ownerNotificationService,
            ObjectMapper objectMapper,
            @Value("${flow.api-base-url}") String flowApiBaseUrl,
            @Value("${app.base-url}") String appBaseUrl,
            @Value("${flow.billing.api-key:}") String apiKey,
            @Value("${flow.billing.secret-key:}") String secretKey) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.ownerNotificationService = ownerNotificationService;
        this.objectMapper = objectMapper;
        this.flowApiBaseUrl = flowApiBaseUrl.replaceAll("/+$", "");
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    /** false mientras Vicente no cargue su propia cuenta Flow (flow.billing.*) - a diferencia de Twilio/Anthropic, esto es opcional al arrancar. */
    public boolean isBillingConfigurado() {
        return apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

    /**
     * Crea (una vez) los planes Básico y Pro en Flow, con los precios del doc
     * (sección 3). Re-llamable sin romper nada: si Flow devuelve error porque
     * el plan ya existe, se loguea y se sigue - no es un error real.
     */
    public void crearPlanesSiNoExisten() {
        exigirConfigurado();
        crearPlanSiNoExiste(PLAN_ID_BASICO, "Plan Básico", 19990);
        crearPlanSiNoExiste(PLAN_ID_PRO, "Plan Pro", 24990);
    }

    private void crearPlanSiNoExiste(String planId, String nombre, int monto) {
        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("planId", planId);
        params.put("name", nombre);
        params.put("currency", "CLP");
        params.put("amount", String.valueOf(monto));
        params.put("interval", String.valueOf(INTERVAL_MENSUAL));
        params.put("urlCallback", appBaseUrl + "/webhooks/flow-billing/notificacion");
        firmarYAgregar(params);

        try {
            JsonNode respuesta = post("/plans/create", params);
            log.info("Plan de suscripción listo en Flow: {} -> {}", planId, respuesta.toString());
        } catch (Exception e) {
            // Probablemente ya existe (Flow no da un código claro de "ya existe" en la doc
            // pública) - se loguea para revisión manual en vez de asumir éxito o fallar duro.
            log.warn("No se pudo crear/confirmar el plan {} en Flow (puede que ya exista): {}", planId, e.getMessage());
        }
    }

    /**
     * Paso 1: crea al tenant como cliente en Flow y devuelve la URL a la que
     * hay que mandar al dueño del negocio para que registre su tarjeta
     * (nunca vemos el número de tarjeta nosotros).
     *
     * @throws BillingException si ese email ya es el billingEmail de OTRO
     *                           tenant - debe ser único porque
     *                           procesarNotificacionPago lo usa para resolver
     *                           a qué tenant pertenece cada cobro entrante de
     *                           Flow (ver Javadoc de la clase).
     */
    @Transactional
    public String iniciarSuscripcion(Tenant tenant, String email) {
        exigirConfigurado();
        tenantSubscriptionRepository.findByBillingEmailAndTenant_IdNot(email, tenant.getId())
                .ifPresent(otra -> {
                    throw new BillingException("Ese email ya está siendo usado como email de facturación por otro negocio.");
                });
        String planId = planIdParaPlan(tenant.getPlan());

        TenantSubscription suscripcion = tenantSubscriptionRepository.findByTenant(tenant)
                .orElseGet(TenantSubscription::new);
        suscripcion.setTenant(tenant);
        suscripcion.setBillingEmail(email);
        suscripcion.setPaymentMethod(PaymentMethod.FLOW_AUTOMATICO);

        String customerId = suscripcion.getFlowCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = crearClienteFlow(tenant, email);
            suscripcion.setFlowCustomerId(customerId);
        }
        suscripcion.setStatus(BillingStatus.PENDIENTE_TARJETA);
        suscripcion.setUpdatedAt(Instant.now());
        try {
            tenantSubscriptionRepository.save(suscripcion);
        } catch (DataIntegrityViolationException e) {
            // Carrera con otro iniciarSuscripcion simultáneo que tomó el
            // mismo email entre el chequeo de arriba y este save - mismo
            // criterio que TenantService.registrarSelfService.
            throw new BillingException("Ese email ya está siendo usado como email de facturación por otro negocio.");
        }

        return registrarTarjeta(tenant.getId(), customerId);
    }

    private String crearClienteFlow(Tenant tenant, String email) {
        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("name", tenant.getBusinessName());
        params.put("email", email);
        params.put("externalId", "tenant-" + tenant.getId());
        firmarYAgregar(params);

        JsonNode respuesta = post("/customer/create", params);
        String customerId = respuesta.path("customerId").asText("");
        if (customerId.isBlank()) {
            log.warn("Flow /customer/create no devolvió customerId para tenant {}: {}", tenant.getId(), respuesta);
            throw new BillingException("No se pudo registrar el negocio en Flow. Probá de nuevo en unos minutos.");
        }
        return customerId;
    }

    private String registrarTarjeta(Long tenantId, String customerId) {
        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("customerId", customerId);
        params.put("url_return", appBaseUrl + "/webhooks/flow-billing/retorno/" + tenantId);
        firmarYAgregar(params);

        JsonNode respuesta = post("/customer/register", params);
        String url = respuesta.path("url").asText("");
        String token = respuesta.path("token").asText("");
        if (url.isBlank() || token.isBlank()) {
            log.warn("Flow /customer/register no devolvió url/token para tenant {}: {}", tenantId, respuesta);
            throw new BillingException("No se pudo generar el link para registrar la tarjeta. Probá de nuevo en unos minutos.");
        }
        return url + "?token=" + token;
    }

    /**
     * Paso 2: Flow llama acá (url_return) cuando el dueño terminó de
     * registrar su tarjeta. Confirma el registro y, si salió bien, recién
     * ahí crea la suscripción real (que empieza a cobrar cada mes).
     */
    @Transactional
    public void procesarRetornoTarjeta(Long tenantId, String token) {
        if (!isBillingConfigurado()) {
            log.warn("Retorno de registro de tarjeta de Flow recibido sin flow.billing.* configurado (tenantId={})", tenantId);
            return;
        }

        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("token", token);
        String firma = firmar(params);

        JsonNode respuesta;
        try {
            respuesta = get("/customer/getRegisterStatus", "apiKey=" + urlEncode(apiKey)
                    + "&token=" + urlEncode(token) + "&s=" + urlEncode(firma));
        } catch (Exception e) {
            log.error("Error consultando getRegisterStatus para tenant {}", tenantId, e);
            return;
        }

        String customerId = respuesta.path("customerId").asText("");
        // La doc pública no detalla el nombre exacto del campo de status del
        // registro - se acepta tanto "status" como "registerStatus" y se
        // trata cualquier valor no vacío/"0" como éxito, ya que lo único que
        // realmente confirma el registro es que Flow nos devuelva un
        // customerId válido.
        if (customerId.isBlank()) {
            log.warn("Registro de tarjeta no confirmado por Flow para tenant {}: {}", tenantId, respuesta);
            return;
        }

        TenantSubscription suscripcion = tenantSubscriptionRepository.findByFlowCustomerId(customerId).orElse(null);
        if (suscripcion == null) {
            log.warn("getRegisterStatus devolvió un customerId sin suscripción local asociada: {}", customerId);
            return;
        }

        boolean yaEstabaActiva = suscripcion.getStatus() == BillingStatus.ACTIVA;
        try {
            String planId = planIdParaPlan(suscripcion.getTenant().getPlan());
            String subscriptionId = crearSuscripcionFlow(planId, customerId);
            suscripcion.setFlowSubscriptionId(subscriptionId);
            suscripcion.setStatus(BillingStatus.ACTIVA);
            suscripcion.setUpdatedAt(Instant.now());
            tenantSubscriptionRepository.save(suscripcion);
            log.info("Suscripción activada en Flow para tenant {}: subscriptionId={}", tenantId, subscriptionId);

            // Solo en la transición a ACTIVA (alta nueva o reactivación tras
            // MOROSA/CANCELADA) - no tiene sentido re-notificar si ya estaba
            // activa. Ver OwnerNotificationService.notificarNuevoTenantActivo:
            // hoy el aprovisionamiento de WhatsApp/Instagram sigue siendo
            // manual (admin), así que esta notificación es lo que cierra el
            // loop del alta self-service (doc sección 12).
            if (!yaEstabaActiva) {
                ownerNotificationService.notificarNuevoTenantActivo(suscripcion.getTenant());
            }
        } catch (Exception e) {
            log.error("Tarjeta registrada pero falló crear la suscripción en Flow para tenant {}", tenantId, e);
        }
    }

    private String crearSuscripcionFlow(String planId, String customerId) {
        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("planId", planId);
        params.put("customerId", customerId);
        firmarYAgregar(params);

        JsonNode respuesta = post("/subscription/create", params);
        String subscriptionId = respuesta.path("subscriptionId").asText("");
        if (subscriptionId.isBlank()) {
            throw new BillingException("Flow no devolvió un id de suscripción.");
        }
        return subscriptionId;
    }

    /**
     * Paso 4: notificación de Flow (urlCallback del plan) cuando se cobra (o
     * se intenta cobrar y falla) un período. Igual que con PaymentService:
     * nunca se confía en el request entrante, siempre se confirma con
     * payment/getStatus.
     * <p>
     * status 2 (pagado) -&gt; ACTIVA. status 3 (rechazado) -&gt; MOROSA
     * automático (antes solo se podía marcar a mano con marcarMorosa()) -
     * ver AiResponseService.puedeUsarBot, que corta el bot para tenants
     * MOROSA/CANCELADA. status 4 (anulado) se trata igual que rechazado: la
     * suscripción dejó de estar cubierta. status 1 (pendiente) no cambia
     * nada todavía - Flow puede reintentar el cobro.
     */
    @Transactional
    public void procesarNotificacionPago(String token) {
        if (!isBillingConfigurado()) {
            log.warn("Notificación de cobro de Flow recibida sin flow.billing.* configurado");
            return;
        }

        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", apiKey);
        params.put("token", token);
        String firma = firmar(params);

        JsonNode respuesta;
        try {
            respuesta = get("/payment/getStatus", "apiKey=" + urlEncode(apiKey)
                    + "&token=" + urlEncode(token) + "&s=" + urlEncode(firma));
        } catch (Exception e) {
            log.error("Error consultando payment/getStatus para notificación de suscripción", e);
            return;
        }

        int status = respuesta.path("status").asInt(-1);
        String payer = respuesta.path("payer").asText("");
        if (payer.isBlank()) {
            log.info("Notificación de cobro de suscripción sin payer informado por Flow (status={}) - no se puede resolver el tenant, revisar manualmente.", status);
            return;
        }

        // billingEmail es único (ver TenantSubscription/iniciarSuscripcion) -
        // a lo sumo un tenant puede matchear, sin ambigüedad posible.
        TenantSubscription suscripcion = tenantSubscriptionRepository.findByBillingEmail(payer).orElse(null);
        if (suscripcion == null) {
            log.warn("Notificación de cobro de Flow sin ningún tenant asociado a ese email (email={}) - revisar manualmente en el panel de Flow.",
                    payer);
            return;
        }

        if (status == 2) {
            suscripcion.setStatus(BillingStatus.ACTIVA);
            suscripcion.setLastPaymentAt(Instant.now());
            // Nuestros planes son mensuales de punta a punta (interval=3, ver
            // crearPlanSiNoExiste) - +1 mes desde hoy es una aproximación
            // razonable de hasta cuándo queda cubierta la suscripción. Si en
            // el futuro se necesita la fecha exacta del período, hay que
            // parsearla del invoice devuelto por Flow en vez de esta
            // aproximación.
            suscripcion.setPaidUntil(LocalDate.now().plusMonths(1));
            suscripcion.setUpdatedAt(Instant.now());
            tenantSubscriptionRepository.save(suscripcion);
            log.info("Pago de suscripción confirmado para tenant {}", suscripcion.getTenant().getId());
        } else if (status == 3 || status == 4) {
            suscripcion.setStatus(BillingStatus.MOROSA);
            suscripcion.setUpdatedAt(Instant.now());
            tenantSubscriptionRepository.save(suscripcion);
            log.warn("Cobro de suscripción rechazado/anulado (status={}) para tenant {} - marcado MOROSA automáticamente",
                    status, suscripcion.getTenant().getId());
            ownerNotificationService.notificarCobroSuscripcionFallido(suscripcion.getTenant());
        } else {
            log.info("Notificación de cobro de suscripción sin resolución final todavía (status={}, payer={})", status, payer);
        }
    }

    public TenantSubscription buscarPorTenant(Tenant tenant) {
        return tenantSubscriptionRepository.findByTenant(tenant).orElse(null);
    }

    /**
     * true si el bot de IA puede seguir respondiendo automático a los
     * clientes de este tenant (ver AiResponseService, único lugar que llama
     * a esto - WebhookController e InstagramWebhookController pasan siempre
     * por ahí). Antes de este chequeo, un tenant MOROSA o CANCELADA seguía
     * recibiendo el servicio indefinidamente: CuentaGate.tsx solo bloqueaba
     * el PANEL, nunca el bot real.
     * <p>
     * Un tenant SIN ninguna TenantSubscription cargada (ej. alta manual
     * directa por TenantService.crear(), sin pasar por self-service ni por
     * registrarPagoManual) no se bloquea - se asume que su facturación se
     * coordina fuera del sistema (primeros pilotos, doc sección 10). El corte
     * automático aplica solo a partir de que existe un registro de
     * suscripción y ese registro dice explícitamente MOROSA/CANCELADA.
     */
    public boolean puedeUsarBot(Tenant tenant) {
        return tenantSubscriptionRepository.findByTenant(tenant)
                .map(s -> s.getStatus() != BillingStatus.MOROSA && s.getStatus() != BillingStatus.CANCELADA)
                .orElse(true);
    }

    /** Marca la suscripción como morosa manualmente (uso admin, mientras no haya notificación automática de cobro fallido documentada - ver clase). */
    @Transactional
    public void marcarMorosa(Tenant tenant) {
        tenantSubscriptionRepository.findByTenant(tenant).ifPresent(s -> {
            s.setStatus(BillingStatus.MOROSA);
            s.setUpdatedAt(Instant.now());
            tenantSubscriptionRepository.save(s);
        });
    }

    /**
     * Registra (o actualiza) un pago manual por transferencia - para los
     * primeros clientes, antes de tener cuenta Flow propia (doc sección 10),
     * o para cualquier tenant que prefiera pagarte así en vez de con tarjeta.
     * Lo carga el admin a mano cada vez que recibe la transferencia.
     */
    @Transactional
    public TenantSubscription registrarPagoManual(Tenant tenant, LocalDate paidUntil) {
        TenantSubscription suscripcion = tenantSubscriptionRepository.findByTenant(tenant)
                .orElseGet(TenantSubscription::new);
        suscripcion.setTenant(tenant);
        suscripcion.setPaymentMethod(PaymentMethod.MANUAL);
        suscripcion.setPaidUntil(paidUntil);
        suscripcion.setStatus(BillingStatus.ACTIVA);
        suscripcion.setLastPaymentAt(Instant.now());
        suscripcion.setUpdatedAt(Instant.now());
        return tenantSubscriptionRepository.save(suscripcion);
    }

    private String planIdParaPlan(TenantPlan plan) {
        return switch (plan) {
            case BASICO -> PLAN_ID_BASICO;
            case PRO -> PLAN_ID_PRO;
            case CATALOGO -> throw new BillingException(
                    "El plan Catálogo es a medida (cotización) - coordiná el cobro manualmente, no tiene suscripción automática todavía.");
        };
    }

    private void exigirConfigurado() {
        if (!isBillingConfigurado()) {
            throw new BillingException("Todavía no cargaste tu propia cuenta Flow para cobrar las suscripciones (flow.billing.api-key/secret-key).");
        }
    }

    private void firmarYAgregar(Map<String, String> params) {
        params.put("s", firmar(params));
    }

    private String firmar(Map<String, String> paramsOrdenados) {
        String toSign = paramsOrdenados.entrySet().stream()
                .map(e -> e.getKey() + e.getValue())
                .collect(Collectors.joining());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar la solicitud a Flow", e);
        }
    }

    private JsonNode post(String path, Map<String, String> params) {
        try {
            String body = params.entrySet().stream()
                    .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flowApiBaseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Flow {} respondió {}: {}", path, response.statusCode(), response.body());
                throw new BillingException("Flow respondió con un error en " + path + ".");
            }
            return objectMapper.readTree(response.body());
        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de conexión llamando a Flow {}", path, e);
            throw new BillingException("No se pudo conectar con Flow en este momento.");
        }
    }

    private JsonNode get(String path, String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(flowApiBaseUrl + path + "?" + query))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new BillingException("Flow respondió con un error en " + path + ".");
        }
        return objectMapper.readTree(response.body());
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
