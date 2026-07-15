package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Cita agendada vía el módulo de agendamiento (plan Pro, ver doc sección 3 y 5.3).
 * Gestionada por SchedulingService a través de las tools agendar_cita /
 * cancelar_reagendar_cita, y por el job de recordatorios automáticos.
 *
 * startTime es LocalDateTime (sin timezone) porque, por ahora, cada negocio
 * opera en un único huso horario (Chile) - simplificación deliberada, igual
 * que el resto del proyecto en esta etapa.
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    /** Número de WhatsApp del cliente que agendó (ver HandoffService para el mismo formato). */
    private String clientPhoneNumber;

    private String service;

    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.CONFIRMADA;

    /** Evita mandar el recordatorio dos veces (ver job de recordatorios). */
    private boolean reminderSent = false;

    private Instant createdAt;
}
