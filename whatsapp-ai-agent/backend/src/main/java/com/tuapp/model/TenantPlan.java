package com.tuapp.model;

/**
 * Plan contratado por el tenant (doc, sección 3). Determina qué tools de
 * function calling puede invocar el agente para ese negocio:
 * - BASICO: solo derivar_a_humano (reactivo puro, doc sección 6).
 * - PRO: además, agendamiento (agendar_cita, cancelar_reagendar_cita) y
 *   recordatorios automáticos.
 * - CATALOGO: además, catálogo sincronizado y link de pago (generar_link_pago).
 */
public enum TenantPlan {
    BASICO,
    PRO,
    CATALOGO
}
