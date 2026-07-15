package com.tuapp.service;

import com.tuapp.model.Appointment;
import com.tuapp.model.AppointmentStatus;
import com.tuapp.model.Availability;
import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import com.tuapp.repository.AppointmentRepository;
import com.tuapp.repository.AvailabilityRepository;
import com.tuapp.repository.ProfessionalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Agenda, cupos y recordatorios (plan Pro, doc secciones 3, 5.2 y 5.3-5.4).
 * La invocan las tools agendar_cita / cancelar_reagendar_cita de
 * AiResponseService, y el job de recordatorios automáticos.
 *
 * v1 deliberadamente simple: no expone "listar horarios libres" como tool
 * (el doc no la define) - si el horario pedido no está disponible, se le
 * devuelve un mensaje al cliente para que proponga otro. Tampoco elige entre
 * profesionales explícitamente: agenda con el primero que tenga ese horario
 * libre (suficiente para negocios de un solo profesional, que son la mayoría
 * del target - ver doc sección 8).
 */
@Slf4j
@Service
public class SchedulingService {

    private final ProfessionalRepository professionalRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    public SchedulingService(
            ProfessionalRepository professionalRepository,
            AvailabilityRepository availabilityRepository,
            AppointmentRepository appointmentRepository) {
        this.professionalRepository = professionalRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Agenda una cita para el primer profesional del tenant que tenga ese
     * horario libre dentro de su disponibilidad semanal.
     *
     * @throws SchedulingException con un mensaje pensado para el cliente si
     *                              no hay ningún profesional disponible en
     *                              ese horario, o si el negocio no tiene
     *                              agenda configurada.
     */
    public Appointment agendarCita(
            Tenant tenant, String numeroCliente, LocalDate fecha, LocalTime hora, String servicio) {

        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new SchedulingException("Ese horario ya pasó, elegí una fecha/hora futura.");
        }

        List<Professional> profesionales = professionalRepository.findByTenantAndActiveTrue(tenant);
        if (profesionales.isEmpty()) {
            throw new SchedulingException("Este negocio todavía no tiene agenda configurada.");
        }

        for (Professional profesional : profesionales) {
            if (estaDentroDeDisponibilidad(profesional, fecha, hora) && !estaOcupado(profesional, inicio)) {
                Appointment cita = new Appointment();
                cita.setTenant(tenant);
                cita.setProfessional(profesional);
                cita.setClientPhoneNumber(numeroCliente);
                cita.setService(servicio);
                cita.setStartTime(inicio);
                cita.setStatus(AppointmentStatus.CONFIRMADA);
                cita.setCreatedAt(Instant.now());
                Appointment guardada = appointmentRepository.save(cita);
                log.info("Cita agendada id={} tenant={} cliente={} inicio={}",
                        guardada.getId(), tenant.getId(), numeroCliente, inicio);
                return guardada;
            }
        }

        throw new SchedulingException("Ese horario no está disponible, ¿tenés otro en mente?");
    }

    /**
     * Cancela o reagenda una cita del cliente. Si se pasan nuevaFecha/nuevaHora,
     * reagenda (valida disponibilidad igual que agendarCita); si no, cancela.
     * Si no se pasa id (el cliente normalmente no sabe el id de su cita),
     * busca la próxima cita CONFIRMADA de ese cliente en este tenant.
     *
     * @throws SchedulingException con un mensaje pensado para el cliente si
     *                              no hay cita que cancelar/reagendar, o si
     *                              el nuevo horario no está disponible.
     */
    public Appointment cancelarOReagendarCita(
            Tenant tenant,
            String numeroCliente,
            Long id,
            LocalDate nuevaFecha,
            LocalTime nuevaHora) {

        Appointment cita = resolverCita(tenant, numeroCliente, id);

        if (nuevaFecha == null || nuevaHora == null) {
            cita.setStatus(AppointmentStatus.CANCELADA);
            log.info("Cita cancelada id={} tenant={} cliente={}", cita.getId(), tenant.getId(), numeroCliente);
            return appointmentRepository.save(cita);
        }

        LocalDateTime nuevoInicio = LocalDateTime.of(nuevaFecha, nuevaHora);
        if (nuevoInicio.isBefore(LocalDateTime.now())) {
            throw new SchedulingException("Ese horario ya pasó, elegí una fecha/hora futura.");
        }
        if (!estaDentroDeDisponibilidad(cita.getProfessional(), nuevaFecha, nuevaHora)
                || estaOcupado(cita.getProfessional(), nuevoInicio)) {
            throw new SchedulingException("Ese horario no está disponible, ¿tenés otro en mente?");
        }

        cita.setStartTime(nuevoInicio);
        cita.setStatus(AppointmentStatus.REAGENDADA);
        cita.setReminderSent(false);
        log.info("Cita reagendada id={} tenant={} cliente={} nuevoInicio={}",
                cita.getId(), tenant.getId(), numeroCliente, nuevoInicio);
        return appointmentRepository.save(cita);
    }

    private Appointment resolverCita(Tenant tenant, String numeroCliente, Long id) {
        if (id != null) {
            Appointment cita = appointmentRepository.findById(id)
                    .orElseThrow(() -> new SchedulingException("No encontré esa cita."));
            if (!cita.getTenant().getId().equals(tenant.getId())
                    || !cita.getClientPhoneNumber().equals(numeroCliente)) {
                throw new SchedulingException("No encontré esa cita.");
            }
            return cita;
        }
        return appointmentRepository
                .findFirstByTenantAndClientPhoneNumberAndStatusOrderByStartTimeAsc(
                        tenant, numeroCliente, AppointmentStatus.CONFIRMADA)
                .orElseThrow(() -> new SchedulingException("No tenés ninguna cita agendada."));
    }

    private boolean estaDentroDeDisponibilidad(Professional profesional, LocalDate fecha, LocalTime hora) {
        return availabilityRepository.findByProfessional(profesional).stream()
                .filter(a -> a.getDayOfWeek() == fecha.getDayOfWeek())
                .anyMatch(a -> !hora.isBefore(a.getStartTime()) && hora.isBefore(a.getEndTime()));
    }

    private boolean estaOcupado(Professional profesional, LocalDateTime inicio) {
        return appointmentRepository
                .existsByProfessionalAndStartTimeAndStatus(profesional, inicio, AppointmentStatus.CONFIRMADA);
    }

    /** Citas confirmadas que empiezan en [desde, hasta) y todavía no tienen recordatorio enviado. */
    public List<Appointment> citasParaRecordar(LocalDateTime desde, LocalDateTime hasta) {
        return appointmentRepository.findByStatusAndStartTimeBetweenAndReminderSentFalse(
                AppointmentStatus.CONFIRMADA, desde, hasta);
    }

    public void marcarRecordatorioEnviado(Appointment cita) {
        cita.setReminderSent(true);
        appointmentRepository.save(cita);
    }

    // ---- Gestión (usada por los endpoints /admin, panel Next.js) ----

    public Professional crearProfesional(Tenant tenant, String name) {
        Professional profesional = new Professional();
        profesional.setTenant(tenant);
        profesional.setName(name);
        profesional.setActive(true);
        return professionalRepository.save(profesional);
    }

    public List<Professional> listarProfesionales(Tenant tenant) {
        return professionalRepository.findByTenant(tenant);
    }

    public Optional<Professional> buscarProfesional(Long id) {
        return professionalRepository.findById(id);
    }

    public Availability crearDisponibilidad(
            Long professionalId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, int slotMinutes) {
        Professional profesional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado: " + professionalId));
        Availability disponibilidad = new Availability();
        disponibilidad.setProfessional(profesional);
        disponibilidad.setDayOfWeek(dayOfWeek);
        disponibilidad.setStartTime(startTime);
        disponibilidad.setEndTime(endTime);
        disponibilidad.setSlotMinutes(slotMinutes);
        return availabilityRepository.save(disponibilidad);
    }

    public List<Availability> listarDisponibilidad(Long professionalId) {
        Professional profesional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado: " + professionalId));
        return availabilityRepository.findByProfessional(profesional);
    }

    /**
     * Lista las citas del tenant, con filtros opcionales por profesional y/o
     * rango de fechas (ambos inclusive, en días). Los usa el panel para la
     * vista de calendario/lista con filtros.
     */
    public List<Appointment> listarCitas(Tenant tenant, Long professionalId, LocalDate desde, LocalDate hasta) {
        Professional profesional = null;
        if (professionalId != null) {
            profesional = professionalRepository.findById(professionalId)
                    .filter(p -> p.getTenant().getId().equals(tenant.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado: " + professionalId));
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fin = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        if (profesional != null && inicio != null && fin != null) {
            return appointmentRepository.findByTenantAndProfessionalAndStartTimeBetween(tenant, profesional, inicio, fin);
        }
        if (profesional != null) {
            return appointmentRepository.findByTenantAndProfessional(tenant, profesional);
        }
        if (inicio != null && fin != null) {
            return appointmentRepository.findByTenantAndStartTimeBetween(tenant, inicio, fin);
        }
        return appointmentRepository.findByTenant(tenant);
    }

    public Optional<Appointment> buscarCita(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Actualización administrativa de una cita desde el panel: cambiar estado
     * (cancelar, marcar completada/no-show, etc.) y/o reagendar a un nuevo
     * horario. A diferencia de {@link #cancelarOReagendarCita}, no exige que
     * coincida el número del cliente (la ejecuta el dueño del negocio o el
     * admin) y no valida la disponibilidad semanal configurada (permite
     * horarios manuales fuera de la agenda, ej. un cliente que llega sin
     * hora) - sí valida que el profesional no tenga otra cita CONFIRMADA en
     * ese mismo horario.
     */
    public Appointment actualizarCita(Long id, AppointmentStatus nuevoStatus, LocalDateTime nuevoInicio) {
        Appointment cita = appointmentRepository.findById(id)
                .orElseThrow(() -> new SchedulingException("No encontré esa cita."));

        if (nuevoInicio != null && !nuevoInicio.equals(cita.getStartTime())) {
            boolean ocupado = appointmentRepository.existsByProfessionalAndStartTimeAndStatusAndIdNot(
                    cita.getProfessional(), nuevoInicio, AppointmentStatus.CONFIRMADA, cita.getId());
            if (ocupado) {
                throw new SchedulingException("Ese profesional ya tiene otra cita confirmada a esa hora.");
            }
            cita.setStartTime(nuevoInicio);
            cita.setReminderSent(false);
            if (nuevoStatus == null) {
                cita.setStatus(AppointmentStatus.REAGENDADA);
            }
        }
        if (nuevoStatus != null) {
            cita.setStatus(nuevoStatus);
        }
        log.info("Cita actualizada desde admin id={} status={} inicio={}", cita.getId(), cita.getStatus(), cita.getStartTime());
        return appointmentRepository.save(cita);
    }

    /**
     * Borra todos los datos de agendamiento de un tenant (citas, disponibilidad
     * y profesionales), en ese orden por las FKs. Lo usa TenantService.eliminar
     * al borrar un negocio completo.
     */
    public void eliminarDatosDeTenant(Tenant tenant) {
        appointmentRepository.deleteByTenant(tenant);
        availabilityRepository.deleteByProfessional_Tenant(tenant);
        professionalRepository.deleteByTenant(tenant);
    }

    // ---- Reportes (plan Pro, doc sección 3: "no-shows evitados, citas por semana, horas peak") ----

    /**
     * Calcula las métricas básicas del módulo de agendamiento para el panel.
     * Si no se pasan fechas, usa las últimas 12 semanas hasta hoy.
     *
     * "noShowsEvitados" es una métrica proxy, no una medición causal real: son
     * las citas con recordatorio enviado que NO terminaron en NO_SHOW. No hay
     * forma de saber con certeza si el recordatorio fue la causa, pero es el
     * indicador que tiene sentido mostrarle al dueño del negocio para
     * justificar el valor del módulo de recordatorios (doc sección 3, plan Pro).
     */
    public ReporteAgendamiento generarReporte(Tenant tenant, LocalDate desde, LocalDate hasta) {
        LocalDate hastaFinal = hasta != null ? hasta : LocalDate.now();
        LocalDate desdeFinal = desde != null ? desde : hastaFinal.minusWeeks(12);

        List<Appointment> citas = listarCitas(tenant, null, desdeFinal, hastaFinal);

        Map<AppointmentStatus, Long> citasPorEstado = citas.stream()
                .collect(Collectors.groupingBy(Appointment::getStatus, Collectors.counting()));

        long noShows = citasPorEstado.getOrDefault(AppointmentStatus.NO_SHOW, 0L);
        long noShowsEvitados = citas.stream()
                .filter(Appointment::isReminderSent)
                .filter(c -> c.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        Map<LocalDate, Long> porSemanaMap = citas.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStartTime().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        TreeMap::new,
                        Collectors.counting()));
        List<CitasPorSemana> citasPorSemana = porSemanaMap.entrySet().stream()
                .map(e -> new CitasPorSemana(e.getKey(), e.getValue()))
                .toList();

        List<HoraPeak> horasPeak = citas.stream()
                .collect(Collectors.groupingBy(c -> c.getStartTime().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new HoraPeak(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(HoraPeak::cantidad).reversed()
                        .thenComparing(HoraPeak::hora))
                .toList();

        return new ReporteAgendamiento(
                desdeFinal, hastaFinal, citas.size(), citasPorEstado,
                noShowsEvitados, noShows, citasPorSemana, horasPeak);
    }

    public record ReporteAgendamiento(
            LocalDate desde,
            LocalDate hasta,
            long totalCitas,
            Map<AppointmentStatus, Long> citasPorEstado,
            long noShowsEvitados,
            long noShows,
            List<CitasPorSemana> citasPorSemana,
            List<HoraPeak> horasPeak) {
    }

    public record CitasPorSemana(LocalDate inicioSemana, long cantidad) {
    }

    public record HoraPeak(int hora, long cantidad) {
    }
}
