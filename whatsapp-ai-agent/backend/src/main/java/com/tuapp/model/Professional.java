package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profesional o "box" con agenda propia dentro de un tenant (doc, sección 3:
 * "varios profesionales o boxes con agenda propia" del plan Pro). Un negocio
 * de una sola persona igual necesita un Professional (el dueño/único
 * atendedor) para poder tener Availability y Appointment.
 */
@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String name;

    private boolean active = true;
}
