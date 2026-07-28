package com.tuapp.service;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notifica al dueño del negocio cuando pasa algo que necesita su atención -
 * hoy: una conversación derivada a humano (doc sección 4: "se notifica al
 * dueño"). Manda un email simple por SMTP si el tenant tiene
 * {@link Tenant#getOwnerEmail()} cargado y el servidor SMTP está configurado
 * (spring.mail.host no vacío, ver application.properties).
 *
 * Deliberadamente "best effort": un fallo de notificación (SMTP mal
 * configurado, email inválido, etc.) nunca debe romper el flujo que la llamó
 * - el handoff en sí (pausar la conversación, que quede visible en el panel)
 * ya pasó antes de intentar notificar, así que como mucho el dueño se entera
 * tarde por el panel en vez de por email.
 */
@Slf4j
@Service
public class OwnerNotificationService {

    private final TenantRepository tenantRepository;
    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlPanel;
    private final String adminEmail;
    private final boolean habilitado;

    public OwnerNotificationService(
            TenantRepository tenantRepository,
            JavaMailSender mailSender,
            @Value("${notifications.email.from:}") String remitente,
            @Value("${panel.public-url:}") String urlPanel,
            @Value("${platform.admin-email:}") String adminEmail,
            @Value("${spring.mail.host:}") String mailHost) {
        this.tenantRepository = tenantRepository;
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlPanel = urlPanel;
        this.adminEmail = adminEmail;
        this.habilitado = mailHost != null && !mailHost.isBlank();
    }

    /** Manda el email de aviso de handoff, si corresponde. No tira excepciones. */
    public void notificarHandoff(Long tenantId, String numeroCliente, String motivo) {
        if (!habilitado || tenantId == null) {
            return;
        }
        tenantRepository.findById(tenantId).ifPresent(tenant -> enviarAvisoHandoff(tenant, numeroCliente, motivo));
    }

    /**
     * Avisa al ADMIN (vos, platform.admin-email - no al dueño del negocio)
     * cuando un tenant self-service (RegistroController) confirma su primer
     * pago y queda ACTIVA (ver SubscriptionBillingService.procesarRetornoTarjeta).
     * Es la señal de "andá a terminar de aprovisionarle el WhatsApp/Instagram":
     * hoy esas credenciales las carga el admin a mano (InstagramController,
     * doc sección 11 - sin flujo OAuth propio en v1), así que sin este aviso
     * un negocio podría pagar y quedar esperando sin que nadie se entere.
     * Best effort, igual que notificarHandoff - nunca rompe el flujo que la llamó.
     */
    public void notificarNuevoTenantActivo(Tenant tenant) {
        if (!habilitado || adminEmail == null || adminEmail.isBlank()) {
            log.info("Tenant {} (id={}) activó su suscripción - platform.admin-email no configurado, "
                    + "avisar manualmente para aprovisionar sus canales.", tenant.getBusinessName(), tenant.getId());
            return;
        }

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Un negocio nuevo confirmó su pago y quedó con la suscripción activa.\n\n");
        cuerpo.append("Negocio: ").append(tenant.getBusinessName()).append("\n");
        cuerpo.append("Plan: ").append(describirPlan(tenant.getPlan())).append("\n");
        cuerpo.append("Email de contacto: ").append(nullASinCargar(tenant.getOwnerEmail())).append("\n");
        cuerpo.append("Id de tenant: ").append(tenant.getId()).append("\n\n");
        cuerpo.append("Falta aprovisionarle el número de WhatsApp y/o la cuenta de Instagram desde el panel admin.");
        if (urlPanel != null && !urlPanel.isBlank()) {
            cuerpo.append("\n\nPanel: ").append(urlPanel);
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(adminEmail);
            mensaje.setSubject("Nuevo cliente activo - " + tenant.getBusinessName());
            mensaje.setText(cuerpo.toString());
            mailSender.send(mensaje);
            log.info("Notificación de nuevo tenant activo enviada al admin (tenant id={})", tenant.getId());
        } catch (MailException e) {
            log.warn("No se pudo enviar la notificación de nuevo tenant activo (tenant id={}): {}",
                    tenant.getId(), e.getMessage());
        }
    }

    /**
     * Avisa al DUEÑO del negocio (no al admin) cuando Flow rechaza/anula el
     * cobro mensual de su suscripción a la plataforma (ver
     * SubscriptionBillingService.procesarNotificacionPago, que ya marcó la
     * suscripción MOROSA antes de llamar acá). A partir de ese momento el bot
     * deja de responder automático en WhatsApp/Instagram (ver
     * AiResponseService.puedeUsarBot) hasta que actualice su método de pago -
     * este email es lo único que se lo explica, porque el corte del bot en sí
     * es silencioso para no exponer el problema de facturación al cliente
     * final. Best effort, igual que el resto de esta clase.
     */
    public void notificarCobroSuscripcionFallido(Tenant tenant) {
        String destinatario = tenant.getOwnerEmail();
        if (!habilitado || destinatario == null || destinatario.isBlank()) {
            log.warn("Suscripción de tenant {} (id={}) quedó MOROSA y no se pudo avisar por email (SMTP no configurado o sin ownerEmail cargado)",
                    tenant.getBusinessName(), tenant.getId());
            return;
        }

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("No pudimos procesar el cobro de tu suscripción mensual.\n\n");
        cuerpo.append("Mientras no se regularice, el asistente de IA dejó de responder automáticamente ");
        cuerpo.append("los mensajes de WhatsApp e Instagram de tu negocio - podés seguir viendo y respondiendo ");
        cuerpo.append("las conversaciones manualmente desde el panel.\n\n");
        cuerpo.append("Entrá al panel para actualizar tu método de pago y reactivar el servicio.");
        if (urlPanel != null && !urlPanel.isBlank()) {
            cuerpo.append("\n\nPanel: ").append(urlPanel);
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject("No pudimos procesar tu pago - " + tenant.getBusinessName());
            mensaje.setText(cuerpo.toString());
            mailSender.send(mensaje);
            log.info("Notificación de cobro fallido enviada a {} (tenant id={})", destinatario, tenant.getId());
        } catch (MailException e) {
            log.warn("No se pudo enviar la notificación de cobro fallido a {} (tenant id={}): {}",
                    destinatario, tenant.getId(), e.getMessage());
        }
    }

    private String describirPlan(TenantPlan plan) {
        return plan != null ? plan.name() : "?";
    }

    private String nullASinCargar(String valor) {
        return valor == null || valor.isBlank() ? "(sin cargar)" : valor;
    }

    private void enviarAvisoHandoff(Tenant tenant, String numeroCliente, String motivo) {
        String destinatario = tenant.getOwnerEmail();
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Un cliente escribió algo que el asistente no pudo resolver solo.\n\n");
        cuerpo.append("Cliente: ").append(numeroCliente).append("\n");
        cuerpo.append("Motivo: ").append(motivo).append("\n\n");
        cuerpo.append("La conversación quedó pausada (el bot no le va a seguir contestando) hasta que la retomes.");
        if (urlPanel != null && !urlPanel.isBlank()) {
            cuerpo.append("\n\nEntrá al panel para responderle: ").append(urlPanel);
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject("Un cliente necesita atención - " + tenant.getBusinessName());
            mensaje.setText(cuerpo.toString());
            mailSender.send(mensaje);
            log.info("Notificación de handoff enviada por email a {} (tenant id={})", destinatario, tenant.getId());
        } catch (MailException e) {
            log.warn("No se pudo enviar la notificación de handoff a {} (tenant id={}): {}",
                    destinatario, tenant.getId(), e.getMessage());
        }
    }
}
