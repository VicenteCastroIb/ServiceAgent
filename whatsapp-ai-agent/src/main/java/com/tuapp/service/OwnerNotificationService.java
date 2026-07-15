package com.tuapp.service;

import com.tuapp.model.Tenant;
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
    private final boolean habilitado;

    public OwnerNotificationService(
            TenantRepository tenantRepository,
            JavaMailSender mailSender,
            @Value("${notifications.email.from:}") String remitente,
            @Value("${panel.public-url:}") String urlPanel,
            @Value("${spring.mail.host:}") String mailHost) {
        this.tenantRepository = tenantRepository;
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlPanel = urlPanel;
        this.habilitado = mailHost != null && !mailHost.isBlank();
    }

    /** Manda el email de aviso de handoff, si corresponde. No tira excepciones. */
    public void notificarHandoff(Long tenantId, String numeroCliente, String motivo) {
        if (!habilitado || tenantId == null) {
            return;
        }
        tenantRepository.findById(tenantId).ifPresent(tenant -> enviarAvisoHandoff(tenant, numeroCliente, motivo));
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
