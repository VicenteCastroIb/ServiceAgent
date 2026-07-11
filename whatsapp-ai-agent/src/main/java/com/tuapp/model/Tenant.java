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
 * businessContext (Semana 2): texto libre con catálogo/precios/horarios/tono
 * que se inyecta directo en el system prompt del agente de IA. Es una
 * simplificación deliberada para esta semana - no hay panel todavía para
 * cargarlo, así que AiResponseService usa un tenant de prueba hardcodeado.
 * TODO Semana 3: reemplazar por catálogo estructurado (entidad Product) +
 * panel web para que el dueño lo cargue, y resolver el Tenant real por
 * número de WhatsApp entrante en vez de un valor fijo.
 *
 * TODO: campos de plan, credenciales Twilio/Meta.
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

    /**
     * Número de WhatsApp del NEGOCIO (el "To" que llega en el webhook de
     * Twilio, no el del cliente). Semana 3: se usa para resolver qué tenant
     * corresponde a cada mensaje entrante.
     * Limitación conocida del sandbox de Twilio: todos los tenants de prueba
     * comparten el mismo número (+14155238886) porque el sandbox es un único
     * número compartido - en producción cada negocio tiene el suyo propio
     * (número dedicado, ver doc sección 2), así que ahí sí discrimina bien.
     */
    @Column(unique = true)
    private String whatsappNumber;

    @Column(columnDefinition = "TEXT")
    private String businessContext;

    private Instant createdAt;
}
