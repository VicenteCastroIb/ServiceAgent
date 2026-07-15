package com.tuapp.service;

import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.ConversationRepository;
import com.tuapp.repository.MessageRepository;
import com.tuapp.repository.PaymentOrderRepository;
import com.tuapp.repository.ProductRepository;
import com.tuapp.repository.TenantRepository;
import com.tuapp.repository.TenantSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona los Tenants (negocios/locales): alta, plan contratado, catálogo,
 * precios, horarios y tono, cargados desde el panel web (ver doc, secciones 2 y 5.2).
 *
 * Semana 3: multi-tenant real. Resuelve qué negocio corresponde a un mensaje
 * entrante según el número de WhatsApp al que le escribieron ("To" del
 * webhook de Twilio), en vez de usar un contexto hardcodeado.
 *
 * Sin panel visual todavía (eso es la otra mitad de la Semana 3) - por ahora
 * los tenants se administran vía TenantController (API) o se cargan con el
 * seeder de más abajo.
 */
@Slf4j
@Service
public class TenantService {

    /**
     * Número compartido del sandbox de Twilio. Todos los tenants de prueba
     * "responden" a este número mientras no tengamos números dedicados por
     * negocio (ver doc, sección 5.6). TODO: sacar este seeder cuando haya
     * panel real para dar de alta negocios.
     */
    private static final String NUMERO_SANDBOX_TWILIO = "whatsapp:+14155238886";

    private final TenantRepository tenantRepository;
    private final SchedulingService schedulingService;
    private final HandoffService handoffService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(
            TenantRepository tenantRepository,
            SchedulingService schedulingService,
            HandoffService handoffService,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProductRepository productRepository,
            PaymentOrderRepository paymentOrderRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.schedulingService = schedulingService;
        this.handoffService = handoffService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.productRepository = productRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Tenant crear(String businessName, String whatsappNumber, String businessContext) {
        return crear(businessName, whatsappNumber, businessContext, TenantPlan.BASICO);
    }

    public Tenant crear(String businessName, String whatsappNumber, String businessContext, TenantPlan plan) {
        Tenant tenant = new Tenant();
        tenant.setBusinessName(businessName);
        tenant.setWhatsappNumber(whatsappNumber);
        tenant.setBusinessContext(businessContext);
        tenant.setPlan(plan);
        tenant.setCreatedAt(Instant.now());
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listar() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> buscarPorId(Long id) {
        return tenantRepository.findById(id);
    }

    public Tenant actualizarContexto(Long id, String businessContext) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setBusinessContext(businessContext);
        return tenantRepository.save(tenant);
    }

    public Tenant actualizarPlan(Long id, TenantPlan plan) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setPlan(plan);
        return tenantRepository.save(tenant);
    }

    /**
     * Borra un negocio y todos sus datos asociados: citas, disponibilidad,
     * profesionales, conversaciones/mensajes, productos, y las conversaciones
     * pausadas en memoria (HandoffService). Se borra en orden hijo-a-padre
     * para no violar las foreign keys. Irreversible - solo el admin puede
     * hacerlo (ver TenantController).
     */
    @Transactional
    public void eliminar(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));

        messageRepository.deleteByConversation_Tenant(tenant);
        conversationRepository.deleteByTenant(tenant);
        schedulingService.eliminarDatosDeTenant(tenant);
        paymentOrderRepository.deleteByTenant(tenant);
        productRepository.deleteByTenant(tenant);
        tenantSubscriptionRepository.deleteByTenant(tenant);
        handoffService.eliminarPorTenant(id);

        tenantRepository.delete(tenant);
        log.info("Tenant eliminado: id={} businessName={}", id, tenant.getBusinessName());
    }

    /**
     * Activa o cambia el acceso al panel del dueño de este negocio. Lo hace
     * el admin (ver SchedulingController/TenantController) - el dueño no se
     * auto-registra. La contraseña se guarda hasheada (BCrypt), nunca en
     * texto plano (ver PanelUserDetailsService, quien la valida).
     */
    public Tenant fijarCredencialesPanel(Long id, String panelUsername, String panelPassword) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setPanelUsername(panelUsername);
        tenant.setPanelPasswordHash(passwordEncoder.encode(panelPassword));
        return tenantRepository.save(tenant);
    }

    /**
     * Credenciales de la tienda WooCommerce del propio comercio (plan
     * Catálogo, ver CatalogSyncService). Las carga el dueño desde su panel -
     * no son secretos nuestros (doc sección 11).
     */
    public Tenant fijarCredencialesWooCommerce(Long id, String url, String consumerKey, String consumerSecret) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setWooCommerceUrl(url);
        tenant.setWooCommerceConsumerKey(consumerKey);
        tenant.setWooCommerceConsumerSecret(consumerSecret);
        return tenantRepository.save(tenant);
    }

    /**
     * Credenciales de la cuenta Flow del propio comercio (plan Catálogo, ver
     * PaymentService). Igual que WooCommerce: son del comercio, la
     * responsabilidad ante el pagador es suya, no nuestra (doc sección 11).
     */
    public Tenant fijarCredencialesFlow(Long id, String apiKey, String secretKey) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setFlowApiKey(apiKey);
        tenant.setFlowSecretKey(secretKey);
        return tenantRepository.save(tenant);
    }

    /**
     * Resuelve el tenant dueño de un número de WhatsApp (el "To" del webhook).
     *
     * @throws IllegalStateException si no hay ningún tenant configurado para
     *                                ese número - se trata como error real
     *                                (no debería pasar en producción con
     *                                números dedicados) y termina derivando
     *                                a humano vía el manejo de errores de
     *                                AiResponseService.
     */
    public Tenant resolverPorNumeroWhatsapp(String numeroWhatsapp) {
        return tenantRepository.findByWhatsappNumber(numeroWhatsapp)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ningún negocio configurado para el número " + numeroWhatsapp));
    }

    /**
     * Resuelve el tenant dueño de una cuenta de Instagram (el "id" de la
     * entry del webhook de Meta), igual que resolverPorNumeroWhatsapp pero
     * para el canal Instagram (doc secciones 3 y 5.1).
     *
     * @throws IllegalStateException si no hay ningún tenant configurado para
     *                                esa cuenta.
     */
    public Tenant resolverPorInstagramAccountId(String instagramAccountId) {
        return tenantRepository.findByInstagramAccountId(instagramAccountId)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ningún negocio configurado para la cuenta de Instagram " + instagramAccountId));
    }

    /**
     * Credenciales de Instagram del negocio, cargadas manualmente desde el
     * panel (mismo patrón que WooCommerce/Flow - ver Tenant.instagramAccountId).
     * accessToken debe ser un token de larga duración (60 días); expiresAt lo
     * usa InstagramTokenRefreshJob para renovarlo antes de que venza.
     */
    public Tenant fijarCredencialesInstagram(Long id, String instagramAccountId, String accessToken, Instant expiresAt) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setInstagramAccountId(instagramAccountId);
        tenant.setInstagramAccessToken(accessToken);
        tenant.setInstagramTokenExpiresAt(expiresAt);
        return tenantRepository.save(tenant);
    }

    /** Actualiza solo el token de Instagram tras un refresh exitoso (ver InstagramTokenRefreshJob). */
    public void actualizarTokenInstagram(Tenant tenant, String nuevoAccessToken, Instant nuevoVencimiento) {
        tenant.setInstagramAccessToken(nuevoAccessToken);
        tenant.setInstagramTokenExpiresAt(nuevoVencimiento);
        tenantRepository.save(tenant);
    }

    /** Tenants con Instagram configurado cuyo token vence antes de la fecha dada (ver InstagramTokenRefreshJob). */
    public List<Tenant> listarConTokenInstagramPorVencer(Instant antesDe) {
        return tenantRepository.findByInstagramAccountIdIsNotNullAndInstagramTokenExpiresAtBefore(antesDe);
    }

    /**
     * Carga un tenant de prueba en el arranque si la base está vacía, para no
     * perder el comportamiento de pruebas que teníamos con el contexto
     * hardcodeado. TODO: sacar esto una vez que exista el panel real.
     */
    @PostConstruct
    void seedTenantDePrueba() {
        if (tenantRepository.count() > 0) {
            return;
        }

        String contexto = """
                Rubro: tienda de ropa casual/streetwear
                Horario de atención: lunes a sábado de 10:00 a 20:00, domingo cerrado
                Tono: cercano, informal pero respetuoso, como si fueras un vendedor joven de la tienda
                Catálogo (resumen):
                - Poleras básicas: $9.990 CLP (colores: negro, blanco, gris)
                - Jockeys bordados: $12.990 CLP
                - Zapatillas urbanas: $34.990 - $49.990 CLP según modelo
                - Envíos a todo Chile, despacho gratis sobre $40.000 CLP
                """;

        // Plan PRO en el tenant de prueba (aunque el rubro no sea "de horas")
        // a propósito, para poder probar el módulo de agendamiento end-to-end
        // en el sandbox, que solo tiene un número/tenant disponible.
        Tenant tenant = crear("Ropa Urbana Ñuñoa", NUMERO_SANDBOX_TWILIO, contexto, TenantPlan.PRO);
        log.info("Tenant de prueba creado para el número sandbox {}", NUMERO_SANDBOX_TWILIO);

        // Login de prueba del "dueño" de este tenant, para poder probar el
        // panel con una cuenta que solo ve este negocio (no el admin, que ve
        // todos). Password de ejemplo - cambiarla desde /admin en un uso real.
        fijarCredencialesPanel(tenant.getId(), "ropaurbana", "qVEUt6wnvmK1YERp");
        log.info("Login de panel de prueba creado para el tenant id={} (usuario: ropaurbana)", tenant.getId());

        // Profesional + disponibilidad de prueba, para poder probar
        // agendar_cita/cancelar_reagendar_cita sin tener que cargarlos a mano
        // primero por la API admin (ver SchedulingService y doc sección 5.3).
        Professional profesional = schedulingService.crearProfesional(tenant, "Atención Ropa Urbana Ñuñoa");
        for (DayOfWeek dia : List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            schedulingService.crearDisponibilidad(
                    profesional.getId(), dia, LocalTime.of(10, 0), LocalTime.of(20, 0), 30);
        }
        log.info("Disponibilidad de prueba creada para el profesional id={}", profesional.getId());
    }
}
