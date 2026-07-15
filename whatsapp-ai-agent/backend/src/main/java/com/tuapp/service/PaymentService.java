package com.tuapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.PaymentOrder;
import com.tuapp.model.PaymentOrderStatus;
import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.PaymentOrderRepository;
import com.tuapp.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * NUEVO (doc sección 5.2). Genera el link de pago por tenant para el plan
 * Catálogo/Ecommerce vía la API de Flow (https://developers.flow.cl),
 * invocado desde la tool generar_link_pago (ver doc, secciones 3, 5.1, 5.3 y
 * 11 - riesgo de intermediación financiera: la cuenta de Flow y su
 * responsabilidad ante el pagador son del comercio, nosotros solo armamos y
 * firmamos el request con las credenciales que el propio dueño carga en su
 * panel).
 *
 * Flujo (ver /payment/create y /payment/getStatus en la doc de Flow):
 * 1) Se valida el carrito contra el catálogo real del tenant y se calcula el
 *    total.
 * 2) Se firma el request con HMAC-SHA256 usando el secretKey del comercio
 *    (los parámetros se ordenan alfabéticamente por clave y se concatenan
 *    clave+valor, tal cual pide la doc de Flow).
 * 3) Flow responde con token+url; el link final de pago es url+"?token="+token.
 * 4) Se guarda una PaymentOrder en PENDIENTE. Cuando el cliente paga, Flow
 *    llama por POST a nuestro webhook de confirmación (urlConfirmation, ver
 *    FlowWebhookController) y ahí se consulta /payment/getStatus para
 *    actualizar el estado real (PAGADA/RECHAZADA/ANULADA).
 *
 * Nota pendiente de validar con el negocio real: Flow exige un email del
 * pagador y nosotros no lo tenemos (el cliente solo escribe por WhatsApp) -
 * por ahora se genera uno sintético a partir del número de teléfono. Si el
 * comercio necesita el email real (para su propia boleta, etc.) hay que
 * pedírselo al cliente en el chat antes de generar el link.
 */
@Slf4j
@Service
public class PaymentService {

    private final ProductRepository productRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper;
    private final String flowApiBaseUrl;
    private final String appBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PaymentService(
            ProductRepository productRepository,
            PaymentOrderRepository paymentOrderRepository,
            ObjectMapper objectMapper,
            @Value("${flow.api-base-url}") String flowApiBaseUrl,
            @Value("${app.base-url}") String appBaseUrl) {
        this.productRepository = productRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.objectMapper = objectMapper;
        this.flowApiBaseUrl = flowApiBaseUrl.replaceAll("/+$", "");
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    /** Item del carrito que arma la IA a partir de lo que pide el cliente. */
    public record ItemCarrito(Long productoId, int cantidad) {
    }

    /**
     * Valida el carrito contra el catálogo real del tenant, genera la orden
     * en Flow y devuelve el link de pago final para mandarle al cliente por
     * WhatsApp.
     *
     * @throws PaymentException si el plan no es CATALOGO, Flow no está
     *                           configurado, el carrito es inválido, o falla
     *                           la llamada a Flow - mensaje pensado para que
     *                           la IA se lo repita tal cual al cliente.
     */
    @Transactional
    public String generarLinkPago(Tenant tenant, String numeroCliente, List<ItemCarrito> itemsCarrito) {
        if (tenant.getPlan() != TenantPlan.CATALOGO) {
            throw new PaymentException("El link de pago solo está disponible en el plan Catálogo.");
        }
        if (!tenant.isFlowConfigurado()) {
            throw new PaymentException("Este negocio todavía no configuró su pasarela de pago.");
        }
        if (itemsCarrito == null || itemsCarrito.isEmpty()) {
            throw new PaymentException("El carrito está vacío.");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<String> nombres = new ArrayList<>();
        for (ItemCarrito item : itemsCarrito) {
            if (item.cantidad() <= 0) {
                throw new PaymentException("La cantidad debe ser mayor a cero.");
            }
            Product producto = productRepository.findById(item.productoId())
                    .filter(p -> p.getTenant().getId().equals(tenant.getId()))
                    .filter(Product::isActive)
                    .orElseThrow(() -> new PaymentException("Uno de los productos del carrito ya no está disponible."));

            total = total.add(producto.getPrice().multiply(BigDecimal.valueOf(item.cantidad())));
            nombres.add(producto.getName() + " x" + item.cantidad());
        }

        String commerceOrder = "T" + tenant.getId() + "-" + System.currentTimeMillis();
        String subject = String.join(", ", nombres);
        if (subject.length() > 200) {
            subject = subject.substring(0, 200);
        }

        // Email sintético: ver nota de clase - Flow lo exige y no tenemos el real.
        String email = numeroCliente.replaceAll("[^0-9]", "") + "@cliente.whatsapp";
        String urlRetorno = (tenant.getPaymentReturnUrl() != null && !tenant.getPaymentReturnUrl().isBlank())
                ? tenant.getPaymentReturnUrl()
                : (tenant.getWooCommerceUrl() != null && !tenant.getWooCommerceUrl().isBlank())
                        ? tenant.getWooCommerceUrl()
                        : appBaseUrl + "/webhooks/flow/retorno";

        Map<String, String> params = new TreeMap<>();
        params.put("apiKey", tenant.getFlowApiKey());
        params.put("commerceOrder", commerceOrder);
        params.put("subject", subject);
        params.put("currency", "CLP");
        params.put("amount", total.setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("email", email);
        params.put("urlConfirmation", appBaseUrl + "/webhooks/flow/confirmacion/" + tenant.getId());
        params.put("urlReturn", urlRetorno);

        String firma = firmar(params, tenant.getFlowSecretKey());
        params.put("s", firma);

        try {
            String body = params.entrySet().stream()
                    .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flowApiBaseUrl + "/payment/create"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Flow payment/create respondió {} para tenant {}: {}",
                        response.statusCode(), tenant.getId(), response.body());
                throw new PaymentException("No se pudo generar el link de pago en este momento.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String url = json.path("url").asText("");
            String token = json.path("token").asText("");
            long flowOrder = json.path("flowOrder").asLong();

            if (url.isBlank() || token.isBlank()) {
                log.warn("Respuesta de Flow sin url/token para tenant {}: {}", tenant.getId(), response.body());
                throw new PaymentException("No se pudo generar el link de pago en este momento.");
            }

            PaymentOrder orden = new PaymentOrder();
            orden.setTenant(tenant);
            orden.setClientPhoneNumber(numeroCliente);
            orden.setCommerceOrder(commerceOrder);
            orden.setFlowToken(token);
            orden.setFlowOrderId(flowOrder);
            orden.setAmount(total);
            paymentOrderRepository.save(orden);

            return url + "?token=" + token;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a Flow para tenant {}", tenant.getId(), e);
            throw new PaymentException("No se pudo generar el link de pago en este momento.");
        }
    }

    /**
     * Se llama desde el webhook de confirmación de Flow (POST a
     * urlConfirmation con el token). Consulta el estado real en
     * /payment/getStatus y actualiza la orden - Flow exige responder 200 en
     * menos de 15 segundos, así que cualquier error se loguea y se traga en
     * vez de propagarse (ver doc de Flow: los errores en la confirmación no
     * afectan el pago ya realizado, solo nuestra propia base local).
     */
    @Transactional
    public void procesarConfirmacion(Long tenantId, String token) {
        PaymentOrder orden = paymentOrderRepository.findByFlowToken(token).orElse(null);
        if (orden == null || !orden.getTenant().getId().equals(tenantId)) {
            log.warn("Confirmación de Flow con token desconocido o de otro tenant (tenantId={})", tenantId);
            return;
        }

        Tenant tenant = orden.getTenant();
        if (!tenant.isFlowConfigurado()) {
            log.warn("Confirmación de Flow para tenant {} sin credenciales cargadas", tenantId);
            return;
        }

        try {
            Map<String, String> paraFirmar = new TreeMap<>();
            paraFirmar.put("apiKey", tenant.getFlowApiKey());
            paraFirmar.put("token", token);
            String firma = firmar(paraFirmar, tenant.getFlowSecretKey());

            String query = "apiKey=" + urlEncode(tenant.getFlowApiKey())
                    + "&token=" + urlEncode(token)
                    + "&s=" + urlEncode(firma);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flowApiBaseUrl + "/payment/getStatus?" + query))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Flow getStatus respondió {} para orden {}", response.statusCode(), orden.getCommerceOrder());
                return;
            }

            JsonNode json = objectMapper.readTree(response.body());
            int status = json.path("status").asInt(-1);
            PaymentOrderStatus nuevoEstado = switch (status) {
                case 2 -> PaymentOrderStatus.PAGADA;
                case 3 -> PaymentOrderStatus.RECHAZADA;
                case 4 -> PaymentOrderStatus.ANULADA;
                default -> PaymentOrderStatus.PENDIENTE;
            };

            orden.setStatus(nuevoEstado);
            if (nuevoEstado != PaymentOrderStatus.PENDIENTE) {
                orden.setConfirmedAt(Instant.now());
            }
            paymentOrderRepository.save(orden);
            log.info("Orden {} actualizada a {} tras confirmación de Flow", orden.getCommerceOrder(), nuevoEstado);
        } catch (Exception e) {
            log.error("Error consultando estado de pago en Flow para orden {}", orden.getCommerceOrder(), e);
        }
    }

    public List<PaymentOrder> listarOrdenes(Tenant tenant) {
        return paymentOrderRepository.findByTenant(tenant);
    }

    /** Firma HMAC-SHA256 de los parámetros, tal cual especifica la doc de Flow: ordenados por clave, concatenados clave+valor. */
    private String firmar(Map<String, String> paramsOrdenados, String secretKey) {
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
            // No debería pasar nunca (HmacSHA256 siempre está disponible en la JVM) -
            // si pasa, es un bug nuestro, no un error de negocio del comercio.
            throw new IllegalStateException("No se pudo firmar la solicitud a Flow", e);
        }
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
