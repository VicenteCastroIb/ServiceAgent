package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Ventana de disponibilidad semanal recurrente de un Professional (ej: "lunes
 * a viernes de 10:00 a 19:00"). SchedulingService la usa para calcular cupos
 * libres, cortando el rango en bloques de slotMinutes y descontando las citas
 * ya agendadas (Appointment) en estado CONFIRMADA.
 */
@Entity
@Table(name = "availabilities")
@Getter
@Setter
@NoArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    /** Duración de cada cupo/turno, en minutos (ej: 30). */
    private int slotMinutes = 30;
}
