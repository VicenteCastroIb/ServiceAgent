package com.tuapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementa derivar_a_humano(motivo): pausa la conversación, notifica al
 * dueño del negocio y la deja en modo manual desde el panel.
 * Se activa cuando el cliente lo pide, hay baja confianza en la respuesta,
 * se detectan reclamos/negociación de precio, o tras varios intentos fallidos
 * (ver doc, sección 4).
 *
 * Semana 2: estado en memoria (se pierde al reiniciar la app) y "notificación"
 * por log. Suficiente para probar el flujo end-to-end.
 * TODO Semana 3: persistir el estado de pausa en Conversation (ya existe la
 * entidad/repositorio), y notificar al dueño de verdad (WhatsApp/email/panel)
 * en vez de solo loguear.
 */
@Slf4j
@Service
public class HandoffService {

    /** Número de WhatsApp del cliente -> motivo de la derivación. */
    private final Map<String, String> conversacionesPausadas = new ConcurrentHashMap<>();

    public void derivarAHumano(String numeroCliente, String motivo) {
        conversacionesPausadas.put(numeroCliente, motivo);
        log.warn("Conversación derivada a humano. Cliente={}, motivo={}", numeroCliente, motivo);
        // TODO Semana 3: notificar al dueño del negocio (push/WhatsApp/email al panel).
    }

    public boolean estaPausada(String numeroCliente) {
        return conversacionesPausadas.containsKey(numeroCliente);
    }

    /** Todas las conversaciones pausadas actualmente (número de cliente -> motivo), para el panel. */
    public Map<String, String> listarPausadas() {
        return Collections.unmodifiableMap(conversacionesPausadas);
    }

    /** Vuelve a activar el bot para ese cliente (ej: el dueño lo reactiva desde el panel). */
    public void reanudar(String numeroCliente) {
        conversacionesPausadas.remove(numeroCliente);
    }
}
