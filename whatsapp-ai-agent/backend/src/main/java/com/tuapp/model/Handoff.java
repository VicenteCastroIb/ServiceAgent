package com.tuapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Conversación pausada por derivar_a_humano (doc sección 4): mientras exista
 * una fila acá para un numeroCliente, el bot no le responde más a ese cliente
 * (ver HandoffService.estaPausada) hasta que el dueño la reanude desde el
 * panel.
 * <p>
 * Persistido en base (antes vivía en un Map en memoria dentro de
 * HandoffService) para que un reinicio/redeploy del backend no reactive por
 * accidente el bot en una conversación que un humano todavía no atendió - ese
 * sería el peor caso posible de esta feature (el cliente esperando que lo
 * atienda una persona, y en cambio el bot le vuelve a contestar solo apenas
 * el proceso arranca de nuevo).
 * <p>
 * numeroCliente es único: solo puede haber un handoff activo por cliente
 * (mismo identificador channel-agnostic que Conversation.clientContact -
 * "whatsapp:+56..." para WhatsApp, "instagram:" + IGSID para Instagram).
 * tenantId puede ser null si el handoff ocurrió antes de poder resolver el
 * tenant (número/cuenta desconocidos) - ver AiResponseService.generarRespuesta -
 * en ese caso solo lo ve el admin (ver HandoffService.listarPausadas).
 */
@Entity
@Table(name = "handoffs")
@Getter
@Setter
@NoArgsConstructor
public class Handoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cliente", unique = true, nullable = false)
    private String numeroCliente;

    /** Null si el handoff ocurrió sin poder resolver el tenant (solo lo ve el admin). */
    private Long tenantId;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    private Instant createdAt;
}
