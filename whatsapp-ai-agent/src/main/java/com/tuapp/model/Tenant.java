package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Representa un negocio/local suscrito a la plataforma (multi-tenant).
 * Contiene el contexto propio del negocio: catálogo, precios, horarios, tono,
 * plan contratado (Básico / Pro / Catálogo) y credenciales de canal (WhatsApp/Instagram).
 *
 * TODO: campos de plan, credenciales Twilio/Meta, tono/prompt del negocio,
 * horarios de atención, etc. se agregan en la Semana 2-3 (ver roadmap doc, sección 9).
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessName;

    private Instant createdAt;
}
