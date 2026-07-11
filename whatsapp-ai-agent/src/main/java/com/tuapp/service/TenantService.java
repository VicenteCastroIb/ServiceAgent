package com.tuapp.service;

import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final PasswordEncoder passwordEncoder;

    public TenantService(
            TenantRepository tenantRepository,
            SchedulingService schedulingService,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.schedulingService = schedulingService;
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
