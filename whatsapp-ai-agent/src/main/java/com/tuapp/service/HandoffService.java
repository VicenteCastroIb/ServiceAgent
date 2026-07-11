package com.tuapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementa derivar_a_humano(motivo): pausa la conversación, notifica al
 * dueño del negocio y la deja en modo manual desde el panel.
 * Se activa cuando el cliente lo pide, hay baja confianza en la respuesta,
 * se detectan reclamos/negociación de precio, o tras varios intentos fallidos
 * (ver doc, sección 4).
 *
 * Semana 2: estado en memoria (se pierde al reiniciar la app) y "notificación"
 * por log. Suficiente para probar el flujo end-to-end.
 * Semana 5: cada handoff queda asociado a un tenantId, para que el panel
 * (login por negocio - ver PanelAuth) solo muestre las conversaciones de SU
 * negocio. tenantId puede ser null si el handoff ocurrió antes de poder
 * resolver el tenant (número de negocio desconocido) - solo el admin lo ve.
 * TODO: persistir el estado de pausa en Conversation (ya existe la
 * entidad/repositorio), y notificar al dueño de verdad (WhatsApp/email/panel)
 * en vez de solo loguear.
 */
@Slf4j
@Service
public class HandoffService {

    /** Número de WhatsApp del cliente -> datos de la derivación. */
    private final Map<String, Handoff> conversacionesPausadas = new ConcurrentHashMap<>();

    public void derivarAHumano(Long tenantId, String numeroCliente, String motivo) {
        conversacionesPausadas.put(numeroCliente, new Handoff(tenantId, motivo));
        log.warn("Conversación derivada a humano. Tenant={}, cliente={}, motivo={}",
                tenantId, numeroCliente, motivo);
        // TODO: notificar al dueño del negocio (push/WhatsApp/email al panel).
    }

    public boolean estaPausada(String numeroCliente) {
        return conversacionesPausadas.containsKey(numeroCliente);
    }

    /**
     * Conversaciones pausadas, para el panel.
     *
     * @param tenantId si no es null, filtra solo las de ese tenant (dueño de
     *                 negocio); si es null, devuelve todas (admin).
     */
    public List<HandoffView> listarPausadas(Long tenantId) {
        return conversacionesPausadas.entrySet().stream()
                .filter(e -> tenantId == null || tenantId.equals(e.getValue().tenantId()))
                .map(e -> new HandoffView(e.getKey(), e.getValue().motivo()))
                .toList();
    }

    /**
     * Vuelve a activar el bot para ese cliente (ej: el dueño lo reactiva
     * desde el panel).
     *
     * @param tenantId si no es null, solo reanuda si el handoff es de ese
     *                 tenant (evita que un dueño reanude la conversación de
     *                 otro negocio adivinando el número de teléfono).
     * @return true si se reanudó, false si no había handoff o era de otro tenant.
     */
    public boolean reanudar(String numeroCliente, Long tenantId) {
        Handoff actual = conversacionesPausadas.get(numeroCliente);
        if (actual == null) {
            return false;
        }
        if (tenantId != null && !tenantId.equals(actual.tenantId())) {
            return false;
        }
        conversacionesPausadas.remove(numeroCliente);
        return true;
    }

    /** Limpia las conversaciones pausadas de un tenant al borrarlo (ver TenantService.eliminar). */
    public void eliminarPorTenant(Long tenantId) {
        conversacionesPausadas.entrySet().removeIf(e -> tenantId.equals(e.getValue().tenantId()));
    }

    private record Handoff(Long tenantId, String motivo) {
    }

    /** Vista sin exponer detalles internos, para el panel. */
    public record HandoffView(String numeroCliente, String motivo) {
    }
}
