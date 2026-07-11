package com.tuapp.service;

/**
 * Error de negocio al generar un link de pago (plan incorrecto, Flow no
 * configurado, producto inexistente, carrito vacío, error de Flow, etc.) -
 * con mensaje pensado para que la IA se lo repita al cliente o el panel lo
 * muestre tal cual (ver PaymentService).
 */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
