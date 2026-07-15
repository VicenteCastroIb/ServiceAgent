package com.tuapp.controller;

import com.tuapp.security.PanelAuth;
import com.tuapp.service.TwilioTemplateException;
import com.tuapp.service.TwilioTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API de administración de plantillas de WhatsApp (Twilio Content API, doc
 * sección 6). Admin-only y sin tenantId en la ruta: como el proyecto usa una
 * sola cuenta de Twilio para todos los negocios (un único WABA - ver
 * TwilioTemplateService), esto es un setup a nivel PLATAFORMA, no por tenant,
 * igual que /admin/billing/planes en BillingController.
 *
 * Flujo pensado: el admin llama POST una vez para crear y mandar a
 * aprobación la plantilla de recordatorio, después consulta GET hasta que
 * el estado sea "approved", y recién ahí copia el contentSid devuelto a la
 * variable de entorno REMINDERS_CONTENT_SID - a partir de eso ReminderJob
 * empieza a usar la plantilla en vez de texto libre (ver MessagingService).
 */
@RestController
@RequestMapping("/admin/whatsapp-templates")
public class WhatsappTemplateController {

    private final TwilioTemplateService twilioTemplateService;

    public WhatsappTemplateController(TwilioTemplateService twilioTemplateService) {
        this.twilioTemplateService = twilioTemplateService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearPlantillaRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            TwilioTemplateService.PlantillaCreada creada = twilioTemplateService.crearYEnviarAprobacion(
                    request.friendlyName(), request.bodyTemplate(), request.variablesEjemplo(), request.categoria());
            return ResponseEntity.ok(creada);
        } catch (TwilioTemplateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{contentSid}")
    public ResponseEntity<?> verEstado(@PathVariable String contentSid) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            TwilioTemplateService.EstadoPlantilla estado = twilioTemplateService.consultarEstado(contentSid);
            return ResponseEntity.ok(estado);
        } catch (TwilioTemplateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    public record CrearPlantillaRequest(
            @NotBlank String friendlyName,
            @NotBlank String bodyTemplate,
            @NotEmpty Map<String, String> variablesEjemplo,
            @NotBlank String categoria) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
