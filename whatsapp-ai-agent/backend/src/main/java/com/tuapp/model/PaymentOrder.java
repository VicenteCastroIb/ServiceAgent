package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orden de pago generada por PaymentService al invocar la tool
 * generar_link_pago (plan Catálogo, doc secciones 3, 5.1, 5.3 y 5.4).
 *
 * Se crea en PENDIENTE al pedirle el link a Flow, y se actualiza cuando Flow
 * llama de vuelta a nuestro webhook de confirmación (urlConfirmation) o
 * cuando se consulta el estado manualmente. Guardamos flowToken para poder
 * resolver a qué orden corresponde cada confirmación entrante.
 */
@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String clientPhoneNumber;

    /** Identificador propio de la orden (lo generamos nosotros, lo manda Flow de vuelta). */
    @Column(unique = true)
    private String commerceOrder;

    /** Token de transacción que asigna Flow - se usa para consultar/confirmar el estado. */
    @Column(unique = true)
    private String flowToken;

    private Long flowOrderId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDIENTE;

    private Instant createdAt = Instant.now();

    private Instant confirmedAt;
}
