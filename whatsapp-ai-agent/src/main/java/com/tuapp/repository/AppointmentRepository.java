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

    void deleteByTenant(Tenant tenant);

    boolean existsByProfessionalAndStartTimeAndStatus(
            Professional professional, LocalDateTime startTime, AppointmentStatus status);

    Optional<Appointment> findFirstByTenantAndClientPhoneNumberAndStatusOrderByStartTimeAsc(
            Tenant tenant, String clientPhoneNumber, AppointmentStatus status);

    /** Para el job de recordatorios: citas confirmadas próximas sin recordatorio enviado. */
    List<Appointment> findByStatusAndStartTimeBetweenAndReminderSentFalse(
            AppointmentStatus status, LocalDateTime desde, LocalDateTime hasta);
}
