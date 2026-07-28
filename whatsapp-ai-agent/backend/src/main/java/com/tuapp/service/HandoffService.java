package com.tuapp.service;

import com.tuapp.model.Handoff;
import com.tuapp.repository.HandoffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementa derivar_a_humano(motivo): pausa la conversación, notifica al
 * dueño del negocio y la deja en modo manual desde el panel.
 * Se activa cuando el cliente lo pide, hay baja confianza en la respuesta,
 * se detectan reclamos/negociación de precio, o tras varios intentos fallidos
 * (ver doc, sección 4).
 *
 * Persistido en Postgres vía HandoffRepository (antes vivía en un Map en
 * memoria de esta misma clase - ver Javadoc de la entidad Handoff): un
 * reinicio/redeploy del backend ya no reactiva por accidente el bot en una
 * conversación que un humano todavía no atendió.
 *
 * Cada handoff queda asociado a un tenantId, para que el panel (login por
 * negocio - ver PanelAuth) solo muestre las conversaciones de SU negocio.
 * tenantId puede ser null si el handoff ocurrió antes de poder resolver el
 * tenant (número de negocio desconocido) - solo el admin lo ve, y no hay a
 * quién notificarle por email en ese caso.
 * La notificación real al dueño (email, ver OwnerNotificationService) es
 * best-effort: si falla o no está configurada, el handoff igual queda
 * registrado y visible en el panel.
 */
@Slf4j
@Service
public class HandoffService {

    private final HandoffRepository handoffRepository;
    private final OwnerNotificationService ownerNotificationService;

    public HandoffService(HandoffRepository handoffRepository, OwnerNotificationService ownerNotificationService) {
        this.handoffRepository = handoffRepository;
        this.ownerNotificationService = ownerNotificationService;
    }

    @Transactional
    public void derivarAHumano(Long tenantId, String numeroCliente, String motivo) {
        derivarAHumano(tenantId, numeroCliente, motivo, true);
    }

    /**
     * Igual que {@link #derivarAHumano(Long, String, String)} pero permite
     * saltar el email de "un cliente necesita atención" - usado por
     * AiResponseService cuando la suscripción del tenant no está activa
     * (ver SubscriptionBillingService.puedeUsarBot): ahí el dueño ya recibió
     * un email específico y más claro sobre el cobro fallido
     * (OwnerNotificationService.notificarCobroSuscripcionFallido), y mandarle
     * además un "cliente necesita atención" por cada conversación que le
     * escriba mientras está MOROSA solo generaría ruido/confusión.
     */
    @Transactional
    public void derivarAHumano(Long tenantId, String numeroCliente, String motivo, boolean notificar) {
        Handoff handoff = handoffRepository.findByNumeroCliente(numeroCliente).orElseGet(Handoff::new);
        handoff.setNumeroCliente(numeroCliente);
        handoff.setTenantId(tenantId);
        handoff.setMotivo(motivo);
        handoff.setCreatedAt(Instant.now());
        handoffRepository.save(handoff);
        log.warn("Conversación derivada a humano. Tenant={}, cliente={}, motivo={}",
                tenantId, numeroCliente, motivo);
        if (notificar) {
            ownerNotificationService.notificarHandoff(tenantId, numeroCliente, motivo);
        }
    }

    public boolean estaPausada(String numeroCliente) {
        return handoffRepository.existsByNumeroCliente(numeroCliente);
    }

    /**
     * Conversaciones pausadas, para el panel.
     *
     * @param tenantId si no es null, filtra solo las de ese tenant (dueño de
     *                 negocio); si es null, devuelve todas (admin).
     */
    public List<HandoffView> listarPausadas(Long tenantId) {
        List<Handoff> handoffs = tenantId != null
                ? handoffRepository.findByTenantId(tenantId)
                : handoffRepository.findAll();
        return handoffs.stream()
                .map(h -> new HandoffView(h.getNumeroCliente(), h.getMotivo()))
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
    @Transactional
    public boolean reanudar(String numeroCliente, Long tenantId) {
        Handoff actual = handoffRepository.findByNumeroCliente(numeroCliente).orElse(null);
        if (actual == null) {
            return false;
        }
        if (tenantId != null && !tenantId.equals(actual.getTenantId())) {
            return false;
        }
        handoffRepository.delete(actual);
        return true;
    }

    /** Limpia las conversaciones pausadas de un tenant al borrarlo (ver TenantService.eliminar). */
    @Transactional
    public void eliminarPorTenant(Long tenantId) {
        handoffRepository.deleteByTenantId(tenantId);
    }

    /** Vista sin exponer detalles internos, para el panel. */
    public record HandoffView(String numeroCliente, String motivo) {
    }
}
