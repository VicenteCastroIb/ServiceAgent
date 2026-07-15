package com.tuapp.service;

/**
 * Error de negocio al gestionar la suscripción de un tenant a la plataforma
 * (billing propio, no confundir con PaymentException que es del plan
 * Catálogo) - mensaje pensado para mostrarse tal cual en el panel de admin.
 */
public class BillingException extends RuntimeException {
    public BillingException(String message) {
        super(message);
    }
}
