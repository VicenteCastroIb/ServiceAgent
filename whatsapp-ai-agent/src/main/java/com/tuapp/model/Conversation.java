package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Conversación con un cliente final por WhatsApp o Instagram, asociada a un Tenant.
 * Guarda el estado (activa / pausada por handoff a humano) usado por HandoffService
 * y por el "modo híbrido" del panel (ver doc, sección 2 y 4).
 *
 * TODO: estado de handoff, canal (WhatsApp/Instagram), referencia al cliente final.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private Instant createdAt;
}
