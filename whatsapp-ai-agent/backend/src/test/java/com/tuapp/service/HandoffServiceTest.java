package com.tuapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests de HandoffService: la lógica de "pausar el bot y que atienda un
 * humano" (doc sección 4) - crítica porque un bug acá significa que el bot
 * le sigue respondiendo a un cliente que ya debería estar hablando con el
 * dueño del negocio, o al revés, que una conversación se quede pausada sin
 * que nadie la retome. Estado en memoria. OwnerNotificationService se mockea
 * sin stubbing (un mock sin comportamiento configurado no hace nada en sus
 * métodos void) - el envío de email en sí no es lo que testea este archivo.
 */
class HandoffServiceTest {

    private HandoffService handoffService;

    @BeforeEach
    void setUp() {
        handoffService = new HandoffService(mock(OwnerNotificationService.class));
    }

    @Test
    void unaConversacionNuevaNoEstaPausada() {
        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isFalse();
    }

    @Test
    void derivarAHumanoPausaLaConversacion() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "el cliente lo pidió");

        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isTrue();
    }

    @Test
    void derivarAHumanoConTenantNuloIgualPausaLaConversacion() {
        // Pasa cuando falla resolver el tenant (ver AiResponseService.generarRespuesta) -
        // el handoff igual debe quedar registrado, solo visible para el admin.
        handoffService.derivarAHumano(null, "whatsapp:+56911111111", "error técnico");

        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isTrue();
    }

    @Test
    void listarPausadasSinTenantIdDevuelveTodas() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");
        handoffService.derivarAHumano(2L, "whatsapp:+56922222222", "motivo B");

        assertThat(handoffService.listarPausadas(null)).hasSize(2);
    }

    @Test
    void listarPausadasConTenantIdFiltraSoloLasDeEseTenant() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");
        handoffService.derivarAHumano(2L, "whatsapp:+56922222222", "motivo B");

        var pausadasTenant1 = handoffService.listarPausadas(1L);

        assertThat(pausadasTenant1).hasSize(1);
        assertThat(pausadasTenant1.get(0).numeroCliente()).isEqualTo("whatsapp:+56911111111");
        assertThat(pausadasTenant1.get(0).motivo()).isEqualTo("motivo A");
    }

    @Test
    void reanudarQuitaLaPausaYDevuelveTrue() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");

        boolean resultado = handoffService.reanudar("whatsapp:+56911111111", 1L);

        assertThat(resultado).isTrue();
        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isFalse();
    }

    @Test
    void reanudarDevuelveFalseSiNoHabiaHandoff() {
        boolean resultado = handoffService.reanudar("whatsapp:+56999999999", 1L);

        assertThat(resultado).isFalse();
    }

    @Test
    void reanudarDevuelveFalseSiElHandoffEsDeOtroTenant() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");

        boolean resultado = handoffService.reanudar("whatsapp:+56911111111", 2L);

        assertThat(resultado).isFalse();
        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isTrue();
    }

    @Test
    void reanudarConTenantIdNuloFuncionaComoAdminSinImportarElTenantDelHandoff() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");

        boolean resultado = handoffService.reanudar("whatsapp:+56911111111", null);

        assertThat(resultado).isTrue();
        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isFalse();
    }

    @Test
    void eliminarPorTenantSoloBorraLasDeEseTenant() {
        handoffService.derivarAHumano(1L, "whatsapp:+56911111111", "motivo A");
        handoffService.derivarAHumano(2L, "whatsapp:+56922222222", "motivo B");

        handoffService.eliminarPorTenant(1L);

        assertThat(handoffService.estaPausada("whatsapp:+56911111111")).isFalse();
        assertThat(handoffService.estaPausada("whatsapp:+56922222222")).isTrue();
    }
}
