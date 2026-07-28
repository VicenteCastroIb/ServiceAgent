package com.tuapp.service;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.ConversationRepository;
import com.tuapp.repository.MessageRepository;
import com.tuapp.repository.PaymentOrderRepository;
import com.tuapp.repository.ProductRepository;
import com.tuapp.repository.TenantRepository;
import com.tuapp.repository.TenantSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
 * Los tenants se administran vía TenantController (API) / panel admin.
 */
@Slf4j
@Service
public class TenantService {

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

    /**
     * Alta SELF-SERVICE de un negocio nuevo (RegistroController, endpoint
     * público /public/registro - doc sección 12, flujo de auto-registro).
     * A diferencia de crear()+fijarCredencialesPanel() (que hace el admin a
     * mano), acá el propio dueño elige su usuario/clave del panel en el
     * momento del registro.
     * <p>
     * Sin whatsappNumber todavía: el número real (dedicado o migrado, doc
     * sección 5.6) se aprovisiona después - no es algo que el dueño tenga a
     * mano al momento de registrarse. Sin plan CATALOGO: ese plan es a medida
     * (cotización, doc sección 3) y no tiene alta self-service - lo valida el
     * caller (RegistroController) antes de llegar acá.
     * <p>
     * La contraseña se guarda hasheada (BCrypt) - nunca en texto plano, mismo
     * criterio que fijarCredencialesPanel.
     *
     * @throws IllegalArgumentException si panelUsername u ownerEmail ya están
     *                                   en uso por otro tenant (chequeo previo
     *                                   + fallback al constraint único de la
     *                                   base para cubrir la carrera entre dos
     *                                   registros simultáneos con el mismo
     *                                   usuario/email). ownerEmail debe ser
     *                                   único porque también se usa como
     *                                   billingEmail al iniciar la suscripción
     *                                   en Flow (ver SubscriptionBillingService) -
     *                                   si dos tenants compartieran email, un
     *                                   cobro de Flow no podría matchearse sin
     *                                   ambigüedad contra uno solo de los dos.
     */
    @Transactional
    public Tenant registrarSelfService(
            String businessName, String ownerEmail, String panelUsername, String panelPassword, TenantPlan plan) {
        if (tenantRepository.findByPanelUsername(panelUsername).isPresent()) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }
        if (tenantRepository.findByOwnerEmail(ownerEmail).isPresent()) {
            throw new IllegalArgumentException("Ese email ya está registrado con otro negocio.");
        }

        Tenant tenant = new Tenant();
        tenant.setBusinessName(businessName);
        tenant.setBusinessContext("");
        tenant.setPlan(plan);
        tenant.setOwnerEmail(ownerEmail);
        tenant.setCreatedAt(Instant.now());
        tenant.setPanelUsername(panelUsername);
        tenant.setPanelPasswordHash(passwordEncoder.encode(panelPassword));

        try {
            return tenantRepository.save(tenant);
        } catch (DataIntegrityViolationException e) {
            // Carrera con otro registro simultáneo que tomó el mismo usuario o
            // email entre el chequeo de arriba y este save - se traduce al
            // mismo error de negocio en vez de dejar escapar un 500 con
            // detalle de la constraint de la base. No se puede distinguir cuál
            // de los dos constraints únicos chocó desde acá (el driver no lo
            // expone de forma portable), así que se da un mensaje genérico.
            throw new IllegalArgumentException("Ese nombre de usuario o email ya está en uso.");
        }
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
     * Email del dueño para notificaciones (ver OwnerNotificationService).
     * Puede pasarse null/vacío para borrarlo - se normaliza a null en ese caso
     * (no a "", para que dos tenants sin email cargado no choquen contra la
     * constraint unique de la columna: la mayoría de las bases tratan NULL
     * como distinto de NULL, pero "" sí colisionaría con otra "").
     *
     * @throws IllegalArgumentException si el email ya está en uso por otro
     *                                   tenant (ver Javadoc de ownerEmail en
     *                                   Tenant y de registrarSelfService).
     */
    public Tenant actualizarOwnerEmail(Long id, String ownerEmail) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));

        String nuevoEmail = (ownerEmail != null && !ownerEmail.isBlank()) ? ownerEmail : null;
        if (nuevoEmail != null) {
            tenantRepository.findByOwnerEmail(nuevoEmail)
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new IllegalArgumentException("Ese email ya está en uso por otro negocio.");
                    });
        }
        tenant.setOwnerEmail(nuevoEmail);

        try {
            return tenantRepository.save(tenant);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ese email ya está en uso por otro negocio.");
        }
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
     * Cambia el usuario/clave del panel de un negocio YA EXISTENTE. Lo hace
     * el admin (ver TenantController) - para el alta inicial de un negocio
     * nuevo que elige su propio usuario/clave, ver registrarSelfService().
     * La contraseña se guarda hasheada (BCrypt), nunca en texto plano (ver
     * PanelUserDetailsService, quien la valida).
     * <p>
     * Incrementa tokenVersion: cualquier JWT ya emitido para el login
     * anterior de este tenant deja de autenticar de inmediato (ver
     * JwtAuthFilter/JwtService.generarTokenTenant), sin esperar a que expire
     * solo - importante en particular si este reset es porque se sospecha
     * que las credenciales anteriores quedaron comprometidas.
     */
    public Tenant fijarCredencialesPanel(Long id, String panelUsername, String panelPassword) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + id));
        tenant.setPanelUsername(panelUsername);
        tenant.setPanelPasswordHash(passwordEncoder.encode(panelPassword));
        tenant.setTokenVersion(tenant.getTokenVersion() + 1);
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
}
