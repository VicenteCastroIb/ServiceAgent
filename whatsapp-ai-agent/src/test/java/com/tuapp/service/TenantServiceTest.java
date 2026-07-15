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
}
