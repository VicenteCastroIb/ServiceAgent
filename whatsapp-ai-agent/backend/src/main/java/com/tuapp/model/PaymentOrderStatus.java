package com.tuapp.model;

/**
 * Estado de una orden de pago generada vía Flow (plan Catálogo, ver doc
 * secciones 3, 5.1 y 5.3). Coincide con los códigos de estado que devuelve
 * Flow en /payment/getStatus: 1 pendiente, 2 pagada, 3 rechazada, 4 anulada
 * (ver PaymentService.procesarConfirmacion).
 */
public enum PaymentOrderStatus {
    PENDIENTE,
    PAGADA,
    RECHAZADA,
    ANULADA
}
