package com.tuapp.service;

import org.springframework.stereotype.Service;

/**
 * Genera la respuesta del agente de IA para un mensaje entrante, usando el
 * contexto propio del tenant (catálogo, precios, horarios, tono) y, según el
 * plan contratado, puede invocar las tools de function calling:
 * derivar_a_humano, agendar_cita, cancelar_reagendar_cita, generar_link_pago
 * (ver doc, secciones 4, 5.3 y 5.4).
 *
 * TODO Semana 2: integrar modelo económico (Gemini Flash-Lite / Claude Haiku)
 * con prompt caching y prompt dinámico por tienda.
 */
@Service
public class AiResponseService {
}
