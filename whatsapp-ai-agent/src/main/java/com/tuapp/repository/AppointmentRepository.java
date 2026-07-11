package com.tuapp.repository;

import com.tuapp.model.Appointment;
import com.tuapp.model.AppointmentStatus;
import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByTenant(Tenant tenant);

    List<Appointment> findByTenantAndProfessional(Tenant tenant, Professional professional);

    List<Appointment> findByTenantAndStartTimeBetween(Tenant tenant, LocalDateTime desde, LocalDateTime hasta);

    List<Appointment> findByTenantAndProfessionalAndStartTimeBetween(
            Tenant tenant, Professional professional, LocalDateTime desde, LocalDateTime hasta);

    void deleteByTenant(Tenant tenant);

    boolean existsByProfessionalAndStartTimeAndStatus(
            Professional professional, LocalDateTime startTime, AppointmentStatus status);

    /** Igual que la anterior, pero excluyendo la propia cita (para validar reagendamientos). */
    boolean existsByProfessionalAndStartTimeAndStatusAndIdNot(
            Professional professional, LocalDateTime startTime, AppointmentStatus status, Long id);

    Optional<Appointment> findFirstByTenantAndClientPhoneNumberAndStatusOrderByStartTimeAsc(
            Tenant tenant, String clientPhoneNumber, AppointmentStatus status);

    /** Para el job de recordatorios: citas confirmadas próximas sin recordatorio enviado. */
    List<Appointment> findByStatusAndStartTimeBetweenAndReminderSentFalse(
            AppointmentStatus status, LocalDateTime desde, LocalDateTime hasta);
}
