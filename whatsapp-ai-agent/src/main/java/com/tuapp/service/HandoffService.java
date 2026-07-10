package com.tuapp.service;

import org.springframework.stereotype.Service;

/**
 * Implementa derivar_a_humano(motivo): pausa la conversación, notifica al
 * dueño del negocio y la deja en modo manual desde el panel.
 * Se activa cuando el cliente lo pide, hay baja confianza en la respuesta,
 * se detectan reclamos/negociación de precio, o tras varios intentos fallidos
 * (ver doc, sección 4).
 *
 * TODO Semana 2: lógica de detección + notificación al dueño.
 */
@Service
public class HandoffService {
}
