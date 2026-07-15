package com.tuapp.service;

import com.tuapp.model.Appointment;
import com.tuapp.model.AppointmentStatus;
import com.tuapp.model.Availability;
import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import com.tuapp.repository.AppointmentRepository;
import com.tuapp.repository.AvailabilityRepository;
import com.tuapp.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la lógica de agendamiento (doc secciones 3, 5.2, 5.3-5.4): la
 * parte más crítica y con más ramas del proyecto, y la que más fácil se
 * rompe silenciosamente con un cambio (choques de horario, disponibilidad,
 * cancelación/reagendamiento). Todo con repositorios mockeados - sin base de
 * datos real, así que corre rápido y no depende de Postgres.
 *
 * Usa un lunes futuro fijo (PROXIMO_LUNES) como referencia de fecha para que
 * los tests no dependan de qué día de la semana sea "hoy" cuando corren.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private SchedulingService schedulingService;

    private Tenant tenant;
    private static final LocalDate PROXIMO_LUNES = proximoLunes();

    @BeforeEach
    void setUp() {
        schedulingService = new SchedulingService(professionalRepository, availabilityRepository, appointmentRepository);
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setBusinessName("Negocio de prueba");
    }

    private static LocalDate proximoLunes() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        while (fecha.getDayOfWeek() != DayOfWeek.MONDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private Professional profesional(long id) {
        Professional p = new Professional();
        p.setId(id);
        p.setTenant(tenant);
        p.setName("Profesional " + id);
        p.setActive(true);
        return p;
    }

    private Availability disponibilidadLunes(LocalTime desde, LocalTime hasta) {
        Availability a = new Availability();
        a.setDayOfWeek(DayOfWeek.MONDAY);
        a.setStartTime(desde);
        a.setEndTime(hasta);
        a.setSlotMinutes(30);
        return a;
    }

    // ---- agendarCita ----

    @Test
    void agendarCita_reservaConElPrimerProfesionalDisponible() {
        Professional p1 = profesional(10L);
        LocalTime hora = LocalTime.of(11, 0);

        when(professionalRepository.findByTenantAndActiveTrue(tenant)).thenReturn(List.of(p1));
        when(availabilityRepository.findByProfessional(p1))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatus(
                eq(p1), eq(LocalDateTime.of(PROXIMO_LUNES, hora)), eq(AppointmentStatus.CONFIRMADA)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        Appointment resultado = schedulingService.agendarCita(tenant, "whatsapp:+56911111111", PROXIMO_LUNES, hora, "Corte");

        assertThat(resultado.getId()).isEqualTo(99L);
        assertThat(resultado.getProfessional()).isEqualTo(p1);
        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.CONFIRMADA);
        assertThat(resultado.getStartTime()).isEqualTo(LocalDateTime.of(PROXIMO_LUNES, hora));
    }

    @Test
    void agendarCita_saltaAlSegundoProfesionalSiElPrimeroNoTieneEseHorario() {
        Professional p1 = profesional(10L);
        Professional p2 = profesional(20L);
        LocalTime hora = LocalTime.of(11, 0);

        when(professionalRepository.findByTenantAndActiveTrue(tenant)).thenReturn(List.of(p1, p2));
        // p1 solo atiende de mañana, no cubre las 11:00 pedidas
        when(availabilityRepository.findByProfessional(p1))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(availabilityRepository.findByProfessional(p2))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatus(
                eq(p2), any(LocalDateTime.class), eq(AppointmentStatus.CONFIRMADA)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.agendarCita(tenant, "whatsapp:+56911111111", PROXIMO_LUNES, hora, "Corte");

        assertThat(resultado.getProfessional()).isEqualTo(p2);
    }

    @Test
    void agendarCita_tiraExcepcionSiElHorarioYaPaso() {
        LocalDate ayer = LocalDate.now().minusDays(1);

        assertThatThrownBy(() ->
                schedulingService.agendarCita(tenant, "whatsapp:+56911111111", ayer, LocalTime.of(10, 0), "Corte"))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("ya pasó");

        verify(professionalRepository, never()).findByTenantAndActiveTrue(any());
    }

    @Test
    void agendarCita_tiraExcepcionSiNoHayProfesionalesConfigurados() {
        when(professionalRepository.findByTenantAndActiveTrue(tenant)).thenReturn(List.of());

        assertThatThrownBy(() ->
                schedulingService.agendarCita(tenant, "whatsapp:+56911111111", PROXIMO_LUNES, LocalTime.of(10, 0), "Corte"))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("no tiene agenda configurada");
    }

    @Test
    void agendarCita_tiraExcepcionSiNingunProfesionalTieneEseHorarioLibre() {
        Professional p1 = profesional(10L);
        LocalTime hora = LocalTime.of(11, 0);

        when(professionalRepository.findByTenantAndActiveTrue(tenant)).thenReturn(List.of(p1));
        when(availabilityRepository.findByProfessional(p1)).thenReturn(List.of());

        assertThatThrownBy(() ->
                schedulingService.agendarCita(tenant, "whatsapp:+56911111111", PROXIMO_LUNES, hora, "Corte"))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("no está disponible");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void agendarCita_tiraExcepcionSiElProfesionalYaEstaOcupado() {
        Professional p1 = profesional(10L);
        LocalTime hora = LocalTime.of(11, 0);

        when(professionalRepository.findByTenantAndActiveTrue(tenant)).thenReturn(List.of(p1));
        when(availabilityRepository.findByProfessional(p1))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatus(
                eq(p1), any(LocalDateTime.class), eq(AppointmentStatus.CONFIRMADA)))
                .thenReturn(true);

        assertThatThrownBy(() ->
                schedulingService.agendarCita(tenant, "whatsapp:+56911111111", PROXIMO_LUNES, hora, "Corte"))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("no está disponible");
    }

    // ---- cancelarOReagendarCita ----

    private Appointment citaExistente(Professional profesional, LocalDateTime inicio, String numeroCliente) {
        Appointment cita = new Appointment();
        cita.setId(5L);
        cita.setTenant(tenant);
        cita.setProfessional(profesional);
        cita.setClientPhoneNumber(numeroCliente);
        cita.setService("Corte");
        cita.setStartTime(inicio);
        cita.setStatus(AppointmentStatus.CONFIRMADA);
        return cita;
    }

    @Test
    void cancelarOReagendarCita_cancelaCuandoNoSePasaNuevaFechaHora() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", 5L, null, null);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.CANCELADA);
    }

    @Test
    void cancelarOReagendarCita_reagendaCuandoElNuevoHorarioEstaLibre() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");
        LocalTime nuevaHora = LocalTime.of(15, 0);

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(availabilityRepository.findByProfessional(p1))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatus(
                eq(p1), eq(LocalDateTime.of(PROXIMO_LUNES, nuevaHora)), eq(AppointmentStatus.CONFIRMADA)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", 5L, PROXIMO_LUNES, nuevaHora);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.REAGENDADA);
        assertThat(resultado.getStartTime()).isEqualTo(LocalDateTime.of(PROXIMO_LUNES, nuevaHora));
        assertThat(resultado.isReminderSent()).isFalse();
    }

    @Test
    void cancelarOReagendarCita_tiraExcepcionSiElNuevoHorarioEstaOcupado() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");
        LocalTime nuevaHora = LocalTime.of(15, 0);

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(availabilityRepository.findByProfessional(p1))
                .thenReturn(List.of(disponibilidadLunes(LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatus(
                eq(p1), any(LocalDateTime.class), eq(AppointmentStatus.CONFIRMADA)))
                .thenReturn(true);

        assertThatThrownBy(() -> schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", 5L, PROXIMO_LUNES, nuevaHora))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void cancelarOReagendarCita_tiraExcepcionSiElNumeroDeClienteNoCoincide() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56900000000");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", 5L, null, null))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("No encontré esa cita");
    }

    @Test
    void cancelarOReagendarCita_sinIdResuelveLaProximaCitaConfirmada() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");

        when(appointmentRepository.findFirstByTenantAndClientPhoneNumberAndStatusOrderByStartTimeAsc(
                tenant, "whatsapp:+56911111111", AppointmentStatus.CONFIRMADA))
                .thenReturn(Optional.of(cita));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", null, null, null);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.CANCELADA);
    }

    @Test
    void cancelarOReagendarCita_sinIdYSinCitaConfirmadaTiraExcepcion() {
        when(appointmentRepository.findFirstByTenantAndClientPhoneNumberAndStatusOrderByStartTimeAsc(
                tenant, "whatsapp:+56911111111", AppointmentStatus.CONFIRMADA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.cancelarOReagendarCita(
                tenant, "whatsapp:+56911111111", null, null, null))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("No tenés ninguna cita");
    }

    // ---- actualizarCita (admin, panel) ----

    @Test
    void actualizarCita_soloCambiaElEstadoSinTocarElHorario() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.actualizarCita(5L, AppointmentStatus.COMPLETADA, null);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.COMPLETADA);
        assertThat(resultado.getStartTime()).isEqualTo(inicio);
        verify(appointmentRepository, never()).existsByProfessionalAndStartTimeAndStatusAndIdNot(
                any(), any(), any(), anyLong());
    }

    @Test
    void actualizarCita_reagendaYPorDefectoQuedaComoReagendada() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");
        LocalDateTime nuevoInicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(16, 0));

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatusAndIdNot(
                p1, nuevoInicio, AppointmentStatus.CONFIRMADA, 5L))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.actualizarCita(5L, null, nuevoInicio);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.REAGENDADA);
        assertThat(resultado.getStartTime()).isEqualTo(nuevoInicio);
        assertThat(resultado.isReminderSent()).isFalse();
    }

    @Test
    void actualizarCita_reagendaConEstadoExplicitoNoLoPisaConReagendada() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");
        LocalDateTime nuevoInicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(16, 0));

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatusAndIdNot(
                p1, nuevoInicio, AppointmentStatus.CONFIRMADA, 5L))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment resultado = schedulingService.actualizarCita(5L, AppointmentStatus.CONFIRMADA, nuevoInicio);

        assertThat(resultado.getStatus()).isEqualTo(AppointmentStatus.CONFIRMADA);
        assertThat(resultado.getStartTime()).isEqualTo(nuevoInicio);
    }

    @Test
    void actualizarCita_tiraExcepcionSiElNuevoHorarioChocaConOtraCitaConfirmada() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");
        LocalDateTime nuevoInicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(16, 0));

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.existsByProfessionalAndStartTimeAndStatusAndIdNot(
                p1, nuevoInicio, AppointmentStatus.CONFIRMADA, 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> schedulingService.actualizarCita(5L, null, nuevoInicio))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("ya tiene otra cita");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void actualizarCita_tiraExcepcionSiLaCitaNoExiste() {
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.actualizarCita(404L, AppointmentStatus.CANCELADA, null))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("No encontré esa cita");
    }

    @Test
    void actualizarCita_noConsultaChoqueSiElNuevoHorarioEsIgualAlActual() {
        Professional p1 = profesional(10L);
        LocalDateTime inicio = LocalDateTime.of(PROXIMO_LUNES, LocalTime.of(10, 0));
        Appointment cita = citaExistente(p1, inicio, "whatsapp:+56911111111");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mismo horario que ya tenía: no debería disparar la validación de choque.
        schedulingService.actualizarCita(5L, AppointmentStatus.NO_SHOW, inicio);

        verify(appointmentRepository, never()).existsByProfessionalAndStartTimeAndStatusAndIdNot(
                any(), any(), any(), anyLong());
    }

    // ---- listarCitas (filtros del panel) ----

    @Test
    void listarCitas_sinFiltrosUsaFindByTenant() {
        when(appointmentRepository.findByTenant(tenant)).thenReturn(List.of());

        schedulingService.listarCitas(tenant, null, null, null);

        verify(appointmentRepository).findByTenant(tenant);
    }

    @Test
    void listarCitas_soloConProfesionalUsaFindByTenantAndProfessional() {
        Professional p1 = profesional(10L);
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(p1));
        when(appointmentRepository.findByTenantAndProfessional(tenant, p1)).thenReturn(List.of());

        schedulingService.listarCitas(tenant, 10L, null, null);

        verify(appointmentRepository).findByTenantAndProfessional(tenant, p1);
    }

    @Test
    void listarCitas_soloConRangoDeFechasUsaFindByTenantAndStartTimeBetween() {
        LocalDate desde = PROXIMO_LUNES;
        LocalDate hasta = PROXIMO_LUNES.plusDays(6);
        when(appointmentRepository.findByTenantAndStartTimeBetween(
                tenant, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        schedulingService.listarCitas(tenant, null, desde, hasta);

        verify(appointmentRepository).findByTenantAndStartTimeBetween(
                tenant, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    @Test
    void listarCitas_conProfesionalYRangoUsaElMetodoCombinado() {
        Professional p1 = profesional(10L);
        LocalDate desde = PROXIMO_LUNES;
        LocalDate hasta = PROXIMO_LUNES.plusDays(6);
        when(professionalRepository.findById(10L)).thenReturn(Optional.of(p1));
        when(appointmentRepository.findByTenantAndProfessionalAndStartTimeBetween(
                tenant, p1, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        schedulingService.listarCitas(tenant, 10L, desde, hasta);

        verify(appointmentRepository).findByTenantAndProfessionalAndStartTimeBetween(
                tenant, p1, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    @Test
    void listarCitas_tiraExcepcionSiElProfesionalEsDeOtroTenant() {
        Professional deOtroTenant = profesional(10L);
        Tenant otroTenant = new Tenant();
        otroTenant.setId(999L);
        deOtroTenant.setTenant(otroTenant);

        when(professionalRepository.findById(10L)).thenReturn(Optional.of(deOtroTenant));

        assertThatThrownBy(() -> schedulingService.listarCitas(tenant, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
