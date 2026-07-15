package com.tuapp.service;

import com.tuapp.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Manda los recordatorios automáticos de cita (plan Pro, doc secciones 3 y
 * 6): mensajes de plantilla "utility" iniciados por el negocio, antes de
 * cada hora agendada.
 *
 * Corre periódicamente y busca citas confirmadas que empiecen dentro de la
 * próxima ventana de anticipación (por defecto 60 minutos) y todavía no
 * tengan el recordatorio marcado como enviado.
 *
 * Si reminders.content-sid está configurado (una plantilla ya aprobada por
 * WhatsApp - ver TwilioTemplateService/WhatsappTemplateController), se manda
 * como plantilla real; si no, se manda como texto libre (válido en el
 * sandbox de Twilio, pero Meta lo rechaza en producción con WABA verificado
 * fuera de la ventana de 24hs - doc sección 6).
 */
@Slf4j
@Component
public class ReminderJob {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final SchedulingService schedulingService;
    private final MessagingService messagingService;
    private final long anticipacionMinutos;
    private final String contentSid;

    public ReminderJob(
            SchedulingService schedulingService,
            MessagingService messagingService,
            @Value("${reminders.anticipacion-minutos:60}") long anticipacionMinutos,
            @Value("${reminders.content-sid:}") String contentSid) {
        this.schedulingService = schedulingService;
        this.messagingService = messagingService;
        this.anticipacionMinutos = anticipacionMinutos;
        this.contentSid = contentSid;
    }

    @Scheduled(fixedDelayString = "${reminders.intervalo-ms:300000}")
    public void enviarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hasta = ahora.plusMinutes(anticipacionMinutos);

        List<Appointment> citas = schedulingService.citasParaRecordar(ahora, hasta);
        for (Appointment cita : citas) {
            try {
                if (contentSid != null && !contentSid.isBlank()) {
                    Map<String, String> variables = Map.of(
                            "1", cita.getTenant().getBusinessName(),
                            "2", cita.getService(),
                            "3", cita.getStartTime().format(FORMATO_HORA));
                    messagingService.enviarWhatsAppConPlantilla(cita.getClientPhoneNumber(), contentSid, variables);
                } else {
                    String texto = "Te recordamos tu hora en %s: %s el %s. Si necesitás cambiarla, escribinos."
                            .formatted(
                                    cita.getTenant().getBusinessName(),
                                    cita.getService(),
                                    cita.getStartTime().format(FORMATO_HORA));
                    messagingService.enviarWhatsApp(cita.getClientPhoneNumber(), texto);
                }
                schedulingService.marcarRecordatorioEnviado(cita);
            } catch (Exception e) {
                // Si falla un recordatorio puntual (número inválido, Twilio caído, etc.)
                // no debe frenar los demás ni reintentar en loop - se loguea y sigue.
                log.error("Falló el recordatorio de la cita id={}: {}", cita.getId(), e.getMessage(), e);
            }
        }

        if (!citas.isEmpty()) {
            log.info("Recordatorios enviados: {}", citas.size());
        }
    }
}
