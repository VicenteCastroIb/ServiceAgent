package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Conversación con un cliente final por WhatsApp o Instagram, asociada a un
 * Tenant. Es la base de la bandeja del panel (doc sección 2: "puede ver y
 * responder los chats ahí mismo") - ver ConversationService/ConversationController.
 * <p>
 * clientContact es el identificador channel-agnostic ya usado en
 * HandoffService/SchedulingService/PaymentService: "whatsapp:+56..." para
 * WhatsApp, "instagram:" + IGSID para Instagram (ver WebhookController /
 * InstagramWebhookController). Único por tenant - el mismo número/cuenta
 * puede escribirle a dos negocios distintos de la plataforma, cada uno con su
 * propia Conversation.
 * <p>
 * El estado de pausa por handoff SIGUE viviendo en HandoffService (en
 * memoria, ver su Javadoc) - deliberadamente no se duplica acá para no tener
 * dos fuentes de verdad; el panel lo consulta aparte y lo combina con esto.
 */
@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "client_contact"}))
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

    @Enumerated(EnumType.STRING)
    private ChannelType channel;

    @Column(name = "client_contact")
    private String clientContact;

    /** Para ordenar la bandeja por recencia sin tener que hacer join+max sobre Message. */
    private Instant lastMessageAt;

    private Instant createdAt;
}
