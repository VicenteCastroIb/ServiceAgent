package com.tuapp.model;

/**
 * Cómo paga un tenant su suscripción a la plataforma (doc secciones 3, 6 y
 * 10). MANUAL: transferencia directa mientras no haya cuenta Flow propia
 * (los primeros clientes, antes de abrir la SpA). FLOW_AUTOMATICO: tarjeta
 * registrada y cobro recurrente vía SubscriptionBillingService.
 */
public enum PaymentMethod {
    MANUAL,
    FLOW_AUTOMATICO
}
