package com.tuapp.service;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.ConversationRepository;
import com.tuapp.repository.MessageRepository;
import com.tuapp.repository.PaymentOrderRepository;
import com.tuapp.repository.ProductRepository;
import com.tuapp.repository.TenantRepository;
import com.tuapp.repository.TenantSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de TenantService: resolución de tenant por canal (WhatsApp/Instagram
 * - la base de todo el multi-tenant, si esto falla un negocio le puede
 * contestar a los clientes de otro) y el borrado en cascada (irreversible,
 * ver TenantService.eliminar). Repositorios mockeados, sin base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private SchedulingService schedulingService;
    @Mock
    private HandoffService handoffService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                tenantRepository,
                schedulingService,
                handoffService,
                conversationRepository,
                messageRepository,
                productRepository,
                paymentOrderRepository,
                tenantSubscriptionRepository,
                passwordEncoder);
    }

    private Tenant tenant(long id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setBusinessName("Negocio " + id);
        return t;
    }

    // ---- resolución por canal ----

    @Test
    void resolverPorNumeroWhatsapp_devuelveElTenantCuandoExiste() {
        Tenant t = tenant(1L);
        when(tenantRepository.findByWhatsappNumber("whatsapp:+56911111111")).thenReturn(Optional.of(t));

        Tenant resultado = tenantService.resolverPorNumeroWhatsapp("whatsapp:+56911111111");

        assertThat(resultado).isEqualTo(t);
    }

    @Test
    void resolverPorNumeroWhatsapp_tiraExcepcionSiNingunTenantTieneEseNumero() {
        when(tenantRepository.findByWhatsappNumber("whatsapp:+56900000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.resolverPorNumeroWhatsapp("whatsapp:+56900000000"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolverPorInstagramAccountId_devuelveElTenantCuandoExiste() {
        Tenant t = tenant(1L);
        when(tenantRepository.findByInstagramAccountId("17841400000000000")).thenReturn(Optional.of(t));

        Tenant resultado = tenantService.resolverPorInstagramAccountId("17841400000000000");

        assertThat(resultado).isEqualTo(t);
    }

    @Test
    void resolverPorInstagramAccountId_tiraExcepcionSiNingunTenantTieneEsaCuenta() {
        when(tenantRepository.findByInstagramAccountId("desconocida")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.resolverPorInstagramAccountId("desconocida"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- credenciales ----

    @Test
    void fijarCredencialesInstagram_actualizaLosTresCamposYGuarda() {
        Tenant t = tenant(1L);
        Instant vencimiento = Instant.now().plusSeconds(60L * 60 * 24 * 60);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.fijarCredencialesInstagram(1L, "cuenta-ig", "token-largo", vencimiento);

        assertThat(resultado.getInstagramAccountId()).isEqualTo("cuenta-ig");
        assertThat(resultado.getInstagramAccessToken()).isEqualTo("token-largo");
        assertThat(resultado.getInstagramTokenExpiresAt()).isEqualTo(vencimiento);
        assertThat(resultado.isInstagramConfigurado()).isTrue();
    }

    @Test
    void fijarCredencialesPanel_guardaLaPasswordHasheadaNoEnTextoPlano() {
        Tenant t = tenant(1L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("miPasswordSecreta")).thenReturn("hash-bcrypt-simulado");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.fijarCredencialesPanel(1L, "dueno1", "miPasswordSecreta");

        assertThat(resultado.getPanelUsername()).isEqualTo("dueno1");
        assertThat(resultado.getPanelPasswordHash()).isEqualTo("hash-bcrypt-simulado");
        verify(passwordEncoder).encode("miPasswordSecreta");
    }

    @Test
    void fijarCredencialesPanel_incrementaTokenVersionParaInvalidarJwtsAnteriores() {
        // Ver JwtAuthFilter/JwtService.generarTokenTenant: un JWT ya emitido
        // para este tenant debe dejar de autenticar apenas se resetean sus
        // credenciales, sin esperar a que expire solo.
        Tenant t = tenant(1L);
        t.setTokenVersion(3);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.fijarCredencialesPanel(1L, "dueno1", "miPasswordSecreta");

        assertThat(resultado.getTokenVersion()).isEqualTo(4);
    }

    @Test
    void actualizarContexto_tiraExcepcionSiElTenantNoExiste() {
        when(tenantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.actualizarContexto(404L, "nuevo contexto"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- borrado en cascada ----

    @Test
    void eliminar_borraTodosLosDatosAsociadosAntesQueElTenant() {
        Tenant t = tenant(1L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));

        tenantService.eliminar(1L);

        // Orden hijo-a-padre: los datos dependientes se borran antes que el
        // tenant, para no violar las foreign keys (ver Javadoc de eliminar()).
        InOrder orden = inOrder(messageRepository, conversationRepository, schedulingService,
                paymentOrderRepository, productRepository, tenantSubscriptionRepository,
                handoffService, tenantRepository);

        orden.verify(messageRepository).deleteByConversation_Tenant(t);
        orden.verify(conversationRepository).deleteByTenant(t);
        orden.verify(schedulingService).eliminarDatosDeTenant(t);
        orden.verify(paymentOrderRepository).deleteByTenant(t);
        orden.verify(productRepository).deleteByTenant(t);
        orden.verify(tenantSubscriptionRepository).deleteByTenant(t);
        orden.verify(handoffService).eliminarPorTenant(1L);
        orden.verify(tenantRepository).delete(t);
    }

    @Test
    void eliminar_tiraExcepcionSiElTenantNoExisteYNoBorraNada() {
        when(tenantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.eliminar(404L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).delete(any());
    }

    // ---- alta ----

    @Test
    void crear_sinPlanExplicitoUsaBasicoPorDefecto() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.crear("Negocio nuevo", "whatsapp:+56933333333", "contexto");

        assertThat(resultado.getPlan()).isEqualTo(TenantPlan.BASICO);
        assertThat(resultado.getWhatsappNumber()).isEqualTo("whatsapp:+56933333333");
    }

    // ---- alta self-service (RegistroController) ----

    @Test
    void registrarSelfService_creaElTenantSinWhatsappNumberConLaPasswordHasheada() {
        when(tenantRepository.findByPanelUsername("dueno-nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("miPasswordSecreta")).thenReturn("hash-bcrypt-simulado");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.registrarSelfService(
                "Negocio nuevo", "dueno@negocio.cl", "dueno-nuevo", "miPasswordSecreta", TenantPlan.PRO);

        assertThat(resultado.getBusinessName()).isEqualTo("Negocio nuevo");
        assertThat(resultado.getOwnerEmail()).isEqualTo("dueno@negocio.cl");
        assertThat(resultado.getPanelUsername()).isEqualTo("dueno-nuevo");
        assertThat(resultado.getPanelPasswordHash()).isEqualTo("hash-bcrypt-simulado");
        assertThat(resultado.getPlan()).isEqualTo(TenantPlan.PRO);
        assertThat(resultado.getWhatsappNumber()).isNull();
    }

    @Test
    void registrarSelfService_tiraExcepcionSiElUsuarioYaExiste() {
        when(tenantRepository.findByPanelUsername("dueno-repetido")).thenReturn(Optional.of(tenant(1L)));

        assertThatThrownBy(() -> tenantService.registrarSelfService(
                "Negocio nuevo", "dueno@negocio.cl", "dueno-repetido", "miPasswordSecreta", TenantPlan.BASICO))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void registrarSelfService_traduceCarreraDeUsuarioDuplicadoAExcepcionDeNegocio() {
        // El chequeo previo no ve nada, pero el save() choca con la
        // constraint única de la base (otro registro tomó el usuario justo
        // en el medio) - debe traducirse a la misma excepción de negocio, no
        // dejar escapar la excepción de persistencia cruda.
        when(tenantRepository.findByPanelUsername("dueno-nuevo")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> tenantService.registrarSelfService(
                "Negocio nuevo", "dueno@negocio.cl", "dueno-nuevo", "miPasswordSecreta", TenantPlan.BASICO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarSelfService_tiraExcepcionSiElEmailYaExiste() {
        // El email debe ser único porque también se usa como billingEmail al
        // iniciar la suscripción en Flow - si dos tenants lo compartieran, un
        // cobro de Flow no podría matchearse sin ambigüedad (ver
        // SubscriptionBillingService.procesarNotificacionPago).
        when(tenantRepository.findByPanelUsername("dueno-nuevo")).thenReturn(Optional.empty());
        when(tenantRepository.findByOwnerEmail("dueno@negocio.cl")).thenReturn(Optional.of(tenant(1L)));

        assertThatThrownBy(() -> tenantService.registrarSelfService(
                "Negocio nuevo", "dueno@negocio.cl", "dueno-nuevo", "miPasswordSecreta", TenantPlan.BASICO))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void registrarSelfService_traduceCarreraDeEmailDuplicadoAExcepcionDeNegocio() {
        when(tenantRepository.findByPanelUsername("dueno-nuevo")).thenReturn(Optional.empty());
        when(tenantRepository.findByOwnerEmail("dueno@negocio.cl")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> tenantService.registrarSelfService(
                "Negocio nuevo", "dueno@negocio.cl", "dueno-nuevo", "miPasswordSecreta", TenantPlan.BASICO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- actualizarOwnerEmail ----

    @Test
    void actualizarOwnerEmail_tiraExcepcionSiOtroTenantYaLoUsa() {
        Tenant t = tenant(1L);
        Tenant otro = tenant(2L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tenantRepository.findByOwnerEmail("repetido@negocio.cl")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> tenantService.actualizarOwnerEmail(1L, "repetido@negocio.cl"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void actualizarOwnerEmail_permiteGuardarSiElEmailEsDelMismoTenant() {
        Tenant t = tenant(1L);
        t.setOwnerEmail("mismo@negocio.cl");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tenantRepository.findByOwnerEmail("mismo@negocio.cl")).thenReturn(Optional.of(t));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.actualizarOwnerEmail(1L, "mismo@negocio.cl");

        assertThat(resultado.getOwnerEmail()).isEqualTo("mismo@negocio.cl");
    }

    @Test
    void actualizarOwnerEmail_normalizaVacioANullEnVezDeCadenaVacia() {
        // "" no debe guardarse tal cual: dos tenants con ownerEmail="" chocarían
        // contra la constraint unique de la columna (a diferencia de NULL, que
        // la mayoría de las bases no considera un duplicado de sí mismo).
        Tenant t = tenant(1L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant resultado = tenantService.actualizarOwnerEmail(1L, "");

        assertThat(resultado.getOwnerEmail()).isNull();
    }
}
