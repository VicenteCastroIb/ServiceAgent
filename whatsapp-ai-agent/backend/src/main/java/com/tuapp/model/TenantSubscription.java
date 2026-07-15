package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Suscripción de un Tenant a la PLATAFORMA (cobro mensual por el plan
 * contratado - Básico/Pro -, doc secciones 3 y 6). Usa la cuenta Flow PROPIA
 * de la plataforma (flow.billing.* en application.properties), distinta de
 * la cuenta Flow de cada tenant que se usa en PaymentOrder para que sus
 * clientes le compren a ÉL por WhatsApp - dos integraciones de Flow
 * completamente separadas, con credenciales distintas.
 *
 * flowCustomerId / flowSubscriptionId: identificadores que devuelve Flow al
 * crear el cliente (/customer/create) y la suscripción (/subscription/create)
 * - ver SubscriptionBillingService.
 */
@Entity
@Table(name = "tenant_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "tenant_id", unique = true)
    private Tenant tenant;

    private String billingEmail;

    private String flowCustomerId;

    private String flowSubscriptionId;

    /** MANUAL (transferencia, mientras no haya cuenta Flow propia) o FLOW_AUTOMATICO (tarjeta + cobro recurrente). */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.MANUAL;

    /**
     * Hasta cuándo está pagada la suscripción. En MANUAL lo carga el admin a
     * mano cada vez que recibe la transferencia (ver
     * SubscriptionBillingService.registrarPagoManual); en FLOW_AUTOMATICO se
     * actualiza solo con cada cobro confirmado (+1 mes, ver
     * procesarNotificacionPago). Se usa para mostrar la vigencia en el panel.
     */
    private LocalDate paidUntil;

    @Enumerated(EnumType.STRING)
    private BillingStatus status = BillingStatus.PENDIENTE_TARJETA;

    private Instant lastPaymentAt;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();
}
