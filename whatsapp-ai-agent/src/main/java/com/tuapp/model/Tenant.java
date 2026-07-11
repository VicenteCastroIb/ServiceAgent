package com.tuapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * simplificación deliberada - no hay entidad Product todavía.
 * TODO: reemplazar por catálogo estructurado (entidad Product) + credenciales
 * propias de Twilio/Meta por tenant (hoy todos comparten las credenciales del
 * .env, ver doc sección 5.6).
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

    /**
     * Plan contratado (doc sección 3). Determina qué tools de function
     * calling puede invocar el agente para este tenant (ver TenantPlan y
     * AiResponseService). Default BASICO: solo derivar_a_humano.
     */
    @Enumerated(EnumType.STRING)
    private TenantPlan plan = TenantPlan.BASICO;

    /**
     * Login propio del dueño del negocio en el panel (Next.js). Nulo mientras
     * el admin no le active el acceso (ver TenantService.fijarCredencialesPanel).
     * panelPasswordHash se guarda con BCrypt, nunca en texto plano - ver
     * PanelUserDetailsService, que es quien los usa para autenticar.
     */
    @Column(unique = true)
    private String panelUsername;

    // @JsonIgnore: el hash nunca debe salir en las respuestas JSON de la API,
    // ni siquiera hasheado - no hay ninguna razón para que el frontend lo vea.
    @JsonIgnore
    private String panelPasswordHash;

    private Instant createdAt;
}
