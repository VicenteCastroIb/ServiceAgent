package com.tuapp.model;

/**
 * Estado de la suscripción de un tenant a la PLATAFORMA (lo que el tenant nos
 * paga a nosotros cada mes - doc sección 3 -, no confundir con PaymentOrder,
 * que es lo que el cliente final le paga a la tienda). Ver
 * TenantSubscription / SubscriptionBillingService.
 */
public enum BillingStatus {
    /** Se creó el cliente en Flow pero todavía no registró una tarjeta. */
    PENDIENTE_TARJETA,
    /** Tarjeta registrada y suscripción creada en Flow, cobrando cada mes. */
    ACTIVA,
    /** Un cobro falló y no se pudo confirmar el último pago del período. */
    MOROSA,
    /** Cancelada (baja del tenant o cancelación manual). */
    CANCELADA
}
