package com.tuapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.PaymentOrderRepository;
import com.tuapp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de las validaciones de PaymentService.generarLinkPago (doc secciones
 * 3, 5.1, 5.2 y 11) - todas las reglas de negocio que tienen que frenar ANTES
 * de llamar a la API real de Flow (plan, credenciales, carrito). No cubre el
 * camino feliz (llamada HTTP real a Flow y guardado de la PaymentOrder),
 * porque el HttpClient está armado adentro de la clase y no es mockeable sin
 * refactorizarla - las validaciones de acá son justamente las que evitan que
 * ese llamado a Flow se dispare con datos inválidos.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                productRepository,
                paymentOrderRepository,
                new ObjectMapper(),
                "https://sandbox.flow.cl/api",
                "http://localhost:8080");
    }

    private Tenant tenantCatalogoConFlowConfigurado() {
        Tenant t = new Tenant();
        t.setId(1L);
        t.setPlan(TenantPlan.CATALOGO);
        t.setFlowApiKey("api-key-de-prueba");
        t.setFlowSecretKey("secret-key-de-prueba");
        return t;
    }

    private Product producto(Tenant tenant, long id, String precio, boolean activo) {
        Product p = new Product();
        p.setId(id);
        p.setTenant(tenant);
        p.setName("Producto " + id);
        p.setPrice(new BigDecimal(precio));
        p.setActive(activo);
        return p;
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElPlanNoEsCatalogo() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setPlan(TenantPlan.PRO);

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(1L, 1))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("plan Catálogo");

        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void generarLinkPago_tiraExcepcionSiFlowNoEstaConfigurado() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setPlan(TenantPlan.CATALOGO);
        // Sin flowApiKey/flowSecretKey cargados.

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(1L, 1))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("no configuró su pasarela de pago");
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElCarritoEsNulo() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();

        assertThatThrownBy(() -> paymentService.generarLinkPago(tenant, "whatsapp:+56911111111", null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("carrito está vacío");
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElCarritoEstaVacio() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();

        assertThatThrownBy(() -> paymentService.generarLinkPago(tenant, "whatsapp:+56911111111", List.of()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("carrito está vacío");
    }

    @Test
    void generarLinkPago_tiraExcepcionSiLaCantidadEsCeroONegativa() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(1L, 0))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("cantidad debe ser mayor a cero");

        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElProductoNoExiste() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(99L, 1))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElProductoEsDeOtroTenant() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();
        Tenant otroTenant = new Tenant();
        otroTenant.setId(2L);
        Product productoDeOtroNegocio = producto(otroTenant, 5L, "9990", true);
        when(productRepository.findById(5L)).thenReturn(Optional.of(productoDeOtroNegocio));

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(5L, 1))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    void generarLinkPago_tiraExcepcionSiElProductoEstaInactivo() {
        Tenant tenant = tenantCatalogoConFlowConfigurado();
        Product productoInactivo = producto(tenant, 5L, "9990", false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(productoInactivo));

        assertThatThrownBy(() -> paymentService.generarLinkPago(
                tenant, "whatsapp:+56911111111", List.of(new PaymentService.ItemCarrito(5L, 1))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ya no está disponible");
    }
}
