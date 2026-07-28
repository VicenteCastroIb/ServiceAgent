package com.tuapp.service;

import com.tuapp.model.Handoff;
import com.tuapp.repository.HandoffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Tests de HandoffService: la lógica de "pausar el bot y que atienda un
 * humano" (doc sección 4) - crítica porque un bug acá significa que el bot
 * le sigue respondiendo a un cliente que ya debería estar hablando con el
 * dueño del negocio, o al revés, que una conversación se quede pausada sin
 * que nadie la retome.
 * <p>
 * HandoffRepository (persistencia real en Postgres, ver Handoff) se mockea
 * acá respaldado por un Map en memoria simple dentro del propio test, para
 * simular su comportamiento sin necesitar una base de datos real - lo que
 * este archivo testea es la LÓGICA de HandoffService (quién puede reanudar
 * qué, filtrado por tenant, etc.), no JPA en sí.
 * <p>
 * OwnerNotificationService se mockea sin stubbing (un mock sin comportamiento
 * configurado no hace nada en sus métodos void) - el envío de email en sí no
 * es lo que testea este archivo.
 */
@ExtendWith(MockitoExtension.class)
class HandoffServiceTest {

    @Mock
    private HandoffRepository handoffRepository;
    @Mock
    private OwnerNotificationService ownerNotificationService;

    private HandoffService handoffService;

    /** Almacén en memoria que respalda el mock de HandoffRepository, simulando la tabla real. */
    private final Map<String, Handoff> almacen = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        almacen.clear();
        handoffService = new HandoffService(handoffRepository, ownerNotificationService);

        lenient().when(handoffRepository.findByNumeroCliente(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(almacen.get(inv.getArgument(0, String.class))));
        lenient().when(handoffRepository.existsByNumeroCliente(anyString()))
                .thenAnswer(inv -> almacen.containsKey(inv.getArgument(0, String.class)));
        lenient().when(handoffRepository.save(any(Handoff.class)))
                .thenAnswer(inv -> {
                    Handoff h = inv.getArgument(0, Handoff.class);
                    almacen.put(h.getNumeroCliente(), h);
                    return h;
                });
        lenient().when(handoffRepository.findByTenantId(any(Long.class)))
                .thenAnswer(inv -> almacen.values().stream()
                        .filter(h -> inv.getArgument(0, Long.class).equals(h.getTenantId()))
                        .toList());
        lenient().when(handoffRepository.findAll())
                .thenAnswer(inv -> List.copyOf(almacen.values()));
        lenient().doAnswer(inv -> {
            Handoff h = inv.getArgument(0, Handoff.class);
            almacen.remove(h.getNumeroCliente());
            return null;
        }).when(handoffRepository).delete(any(Handoff.class));
        lenient().doAnswer(inv -> {
            Long tenantId = inv.getArgument(0, Long.class);
            almacen.values().removeIf(h -> tenantId.equals(h.getTenantId()));
            return null;
        }).when(handoffRepository).deleteByTenantId(any(Long.class));
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
