package com.tuapp.service;

/**
 * Error de agendamiento con un mensaje pensado para mostrarle directo al
 * cliente (horario ocupado, cita inexistente, etc.) - distinto de una falla
 * técnica real. AiResponseService la captura aparte de las excepciones
 * genéricas para devolvérsela a Claude como resultado de la tool en vez de
 * derivar a humano.
 */
public class SchedulingException extends RuntimeException {
    public SchedulingException(String mensaje) {
        super(mensaje);
    }
}
