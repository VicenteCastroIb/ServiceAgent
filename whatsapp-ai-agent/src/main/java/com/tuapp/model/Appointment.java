package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Cita agendada vía el módulo de agendamiento (plan Pro, ver doc sección 3 y 5.3).
 * Gestionada por SchedulingService a través de las tools agendar_cita / cancelar_reagendar_cita.
 *
 * TODO: profesional/box asignado, estado (confirmada/cancelada/reagendada),
 * recordatorio enviado (sí/no), servicio.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private Instant scheduledAt;
}
