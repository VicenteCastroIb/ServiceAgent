package com.tuapp.controller;

import com.tuapp.model.Appointment;
import com.tuapp.model.AppointmentStatus;
import com.tuapp.model.Availability;
import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.SchedulingException;
import com.tuapp.service.SchedulingService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * API de administración del módulo de agendamiento (plan Pro, doc secciones
 * 3, 5.2 y 5.3-5.4): profesionales/boxes, su disponibilidad semanal, y las
 * citas agendadas. La consume el panel Next.js.
 *
 * Protegida por JWT igual que {@link TenantController} (ver SecurityConfig).
 * Autorización por tenant (ver PanelAuth): el admin ve/edita todo; el dueño
 * de un negocio solo lo suyo. En las rutas de disponibilidad (por
 * professionalId, no tenantId) se resuelve primero el tenant dueño del
 * profesional para poder aplicar el mismo chequeo.
 */
@RestController
@RequestMapping("/admin")
public class SchedulingController {

    private final TenantService tenantService;
    private final SchedulingService schedulingService;

    public SchedulingController(TenantService tenantService, SchedulingService schedulingService) {
        this.tenantService = tenantService;
        this.schedulingService = schedulingService;
    }

    @GetMapping("/tenants/{tenantId}/professionals")
    public ResponseEntity<List<Professional>> listarProfesionales(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schedulingService.listarProfesionales(tenant));
    }

    @PostMapping("/tenants/{tenantId}/professionals")
    public ResponseEntity<Professional> crearProfesional(
            @PathVariable Long tenantId, @Valid @RequestBody CrearProfesionalRequest request) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        Professional profesional = schedulingService.crearProfesional(tenant, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(profesional);
    }

    @GetMapping("/professionals/{professionalId}/availability")
    public ResponseEntity<List<Availability>> listarDisponibilidad(@PathVariable Long professionalId) {
        return schedulingService.buscarProfesional(professionalId)
                .map(profesional -> {
                    if (!PanelAuth.puedeAcceder(profesional.getTenant().getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<List<Availability>>build();
                    }
                    return ResponseEntity.ok(schedulingService.listarDisponibilidad(professionalId));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/professionals/{professionalId}/availability")
    public ResponseEntity<Availability> crearDisponibilidad(
            @PathVariable Long professionalId, @Valid @RequestBody CrearDisponibilidadRequest request) {
        return schedulingService.buscarProfesional(professionalId)
                .map(profesional -> {
                    if (!PanelAuth.puedeAcceder(profesional.getTenant().getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Availability>build();
                    }
                    Availability disponibilidad = schedulingService.crearDisponibilidad(
                            professionalId,
                            request.dayOfWeek(),
                            request.startTime(),
                            request.endTime(),
                            request.slotMinutes() != null ? request.slotMinutes() : 30);
                    return ResponseEntity.status(HttpStatus.CREATED).body(disponibilidad);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tenants/{tenantId}/appointments")
    public ResponseEntity<List<Appointment>> listarCitas(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(schedulingService.listarCitas(tenant, professionalId, desde, hasta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/tenants/{tenantId}/appointments/reporte")
    public ResponseEntity<SchedulingService.ReporteAgendamiento> generarReporte(
            @PathVariable Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schedulingService.generarReporte(tenant, desde, hasta));
    }

    @PatchMapping("/appointments/{id}")
    public ResponseEntity<?> actualizarCita(@PathVariable Long id, @RequestBody ActualizarCitaRequest request) {
        return schedulingService.buscarCita(id)
                .map(cita -> {
                    if (!PanelAuth.puedeAcceder(cita.getTenant().getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) null);
                    }
                    try {
                        Appointment actualizada =
                                schedulingService.actualizarCita(id, request.status(), request.startTime());
                        return ResponseEntity.ok((Object) actualizada);
                    } catch (SchedulingException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body((Object) new ErrorResponse(e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Tenant buscarTenant(Long tenantId) {
        return tenantService.buscarPorId(tenantId).orElse(null);
    }

    public record CrearProfesionalRequest(@NotBlank String name) {
    }

    public record CrearDisponibilidadRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Positive Integer slotMinutes) {
    }

    public record ActualizarCitaRequest(AppointmentStatus status, LocalDateTime startTime) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
