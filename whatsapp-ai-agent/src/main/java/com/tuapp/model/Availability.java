package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cupo/hora disponible de un profesional o box de un Tenant (plan Pro).
 * Usado por SchedulingService para ofrecer horarios al agendar_cita.
 *
 * TODO: profesional/box, día/hora, duración, estado (libre/tomado).
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

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}
