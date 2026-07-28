package com.tuapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuapp.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Representa un negocio/local suscrito a la plataforma (multi-tenant).
 * Contiene el contexto propio del negocio: catálogo, precios, horarios, tono,
 * plan contratado (Básico / Pro / Catálogo) y credenciales de canal (WhatsApp/Instagram).
 *
 * businessContext (Semana 2): texto libre con catálogo/precios/horarios/tono
 * que se inyecta directo en el system prompt del agente de IA. Es una
 * simplificación deliberada - no hay entidad Product todavía.
 * TODO: reemplazar por catálogo estructurado (entidad Product) + credenciales
 * propias de Twilio/Meta por tenant (hoy todos comparten las credenciales del
 * .env, ver doc sección 5.6).
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessName;

    /**
     * Número de WhatsApp del NEGOCIO (el "To" que llega en el webhook de
     * Twilio, no el del cliente). Semana 3: se usa para resolver qué tenant
     * corresponde a cada mensaje entrante.
     * Limitación conocida del sandbox de Twilio: todos los tenants de prueba
     * comparten el mismo número (+14155238886) porque el sandbox es un único
     * número compartido - en producción cada negocio tiene el suyo propio
     * (número dedicado, ver doc sección 2), así que ahí sí discrimina bien.
     */
    @Column(unique = true)
    private String whatsappNumber;

    @Column(columnDefinition = "TEXT")
    private String businessContext;

    /**
     * Plan contratado (doc sección 3). Determina qué tools de function
     * calling puede invocar el agente para este tenant (ver TenantPlan y
     * AiResponseService). Default BASICO: solo derivar_a_humano.
     */
    @Enumerated(EnumType.STRING)
    private TenantPlan plan = TenantPlan.BASICO;

    /**
     * Login propio del dueño del negocio en el panel (Next.js). Nulo mientras
     * el admin no le active el acceso (ver TenantService.fijarCredencialesPanel).
     * panelPasswordHash se guarda con BCrypt, nunca en texto plano - ver
     * PanelUserDetailsService, que es quien los usa para autenticar.
     */
    @Column(unique = true)
    private String panelUsername;

    // @JsonIgnore: el hash nunca debe salir en las respuestas JSON de la API,
    // ni siquiera hasheado - no hay ninguna razón para que el frontend lo vea.
    @JsonIgnore
    private String panelPasswordHash;

    /**
     * Se incrementa cada vez que se resetean las credenciales del panel de
     * este negocio (ver TenantService.fijarCredencialesPanel). Va embebido en
     * el JWT de login de este tenant (ver JwtService.generarTokenTenant) y se
     * revalida contra este valor en cada request (ver JwtAuthFilter) - así,
     * un JWT ya emitido deja de servir inmediatamente después de un reset de
     * credenciales, sin tener que esperar a que expire solo (por defecto a
     * los 60 minutos). Si el tenant se borra directamente, el JWT también
     * deja de servir porque la fila entera desaparece.
     */
    private int tokenVersion = 0;

    /**
     * Credenciales del plan Catálogo/Ecommerce (doc secciones 3, 5.1 y 5.3),
     * propias de cada comercio - no son secretos nuestros, son las claves del
     * comercio con SU tienda WooCommerce y SU cuenta de Flow. Por eso el
     * riesgo de intermediación financiera es del comercio, no de la
     * plataforma (doc sección 11): nosotros solo armamos el link de pago con
     * las credenciales que el propio dueño carga en su panel.
     *
     * @JsonIgnore en consumer secret / api key / secret key: nunca deben salir
     * en las respuestas JSON. wooCommerceUrl y wooCommerceConsumerKey no son
     * tan sensibles pero se ocultan igual por consistencia y porque el
     * frontend no los necesita de vuelta (solo confirma que están cargados
     * vía isWooCommerceConfigurado()/isFlowConfigurado()).
     *
     * @Convert(EncryptedStringConverter) en las credenciales reales (consumer
     * key/secret, api key/secret key): @JsonIgnore solo las saca de las
     * respuestas de la API, no protege nada si la base (o un backup) se
     * filtra - con esto quedan cifradas en la columna misma (AES-256-GCM, ver
     * la clase). wooCommerceUrl no se cifra: no es un secreto, es la URL
     * pública de la tienda.
     *
     * columnDefinition TEXT en los campos cifrados: el valor guardado es
     * base64(iv + ciphertext + tag), más largo que el texto plano original -
     * con el VARCHAR(255) por defecto de Hibernate, un token ya largo de por
     * sí (ej. un access token de Instagram) se hubiera truncado en silencio al
     * guardar.
     */
    @JsonIgnore
    private String wooCommerceUrl;

    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String wooCommerceConsumerKey;

    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String wooCommerceConsumerSecret;

    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String flowApiKey;

    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String flowSecretKey;

    /** URL de retorno tras pagar en Flow (opcional). Si no se carga, se usa wooCommerceUrl o una página genérica propia. */
    private String paymentReturnUrl;

    /**
     * Canal Instagram (doc secciones 3 y 5.1: "Instagram vía Graph API directa
     * de Meta", incluido desde el plan Básico). instagramAccountId es el id
     * de la cuenta profesional de Instagram del negocio - lo manda Meta en
     * cada webhook entrante (entry.id) y se usa para resolver el tenant, igual
     * que whatsappNumber para WhatsApp. El access token se obtiene fuera de
     * esta app (Meta App del dueño del negocio o de la plataforma, según cómo
     * se conecte) y se carga acá manualmente desde el panel - mismo patrón
     * simple que WooCommerce/Flow (doc sección 11: no hay flujo OAuth propio
     * en v1). Es de vida corta (60 días) - InstagramTokenRefreshJob lo
     * refresca antes de que venza usando instagramTokenExpiresAt.
     *
     * instagramAccountId NO se cifra a propósito: es @Column(unique = true) y
     * se busca por igualdad (InstagramWebhookController resuelve el tenant
     * por este campo en cada mensaje entrante) - EncryptedStringConverter usa
     * un IV aleatorio por valor, así que un mismo id cifrado dos veces da
     * ciphertexts distintos y rompería tanto la unicidad como la búsqueda.
     * instagramAccessToken sí es un secreto real (permite postear/leer como
     * la cuenta de Instagram del negocio) y no se busca por igualdad en
     * ningún lado, así que se cifra sin este problema.
     */
    @Column(unique = true)
    private String instagramAccountId;

    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String instagramAccessToken;

    private Instant instagramTokenExpiresAt;

    /**
     * Email del dueño del negocio: para notificarle cuando una conversación
     * se deriva a humano (doc sección 4, ver OwnerNotificationService), y
     * también el que se usa como billingEmail al iniciar su suscripción en
     * Flow (ver SubscriptionBillingService). Único a propósito (nulo
     * permitido: varios tenants sin email cargado no chocan entre sí, la
     * mayoría de las bases de datos tratan NULL como distinto de NULL en una
     * constraint unique) - un mismo email en dos tenants distintos hacía que
     * SubscriptionBillingService.procesarNotificacionPago no pudiera saber
     * con certeza a cuál de los dos pertenecía un cobro de Flow (ver
     * TenantService.registrarSelfService/actualizarOwnerEmail, que validan
     * esto antes de guardar).
     */
    @Column(unique = true)
    private String ownerEmail;

    private Instant createdAt;

    /** Para que el panel sepa si ya está configurado, sin exponer las claves reales. */
    @Transient
    public boolean isWooCommerceConfigurado() {
        return notBlank(wooCommerceUrl) && notBlank(wooCommerceConsumerKey) && notBlank(wooCommerceConsumerSecret);
    }

    /** Para que el panel sepa si ya está configurado, sin exponer las claves reales. */
    @Transient
    public boolean isFlowConfigurado() {
        return notBlank(flowApiKey) && notBlank(flowSecretKey);
    }

    /** Para que el panel sepa si ya está configurado, sin exponer el token real. */
    @Transient
    public boolean isInstagramConfigurado() {
        return notBlank(instagramAccountId) && notBlank(instagramAccessToken);
    }

    private static boolean notBlank(String valor) {
        return valor != null && !valor.isBlank();
    }
}
