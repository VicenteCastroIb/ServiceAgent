package com.tuapp.model;

/**
 * Quién escribió un Message. CLIENTE para los entrantes; BOT para las
 * respuestas automáticas de AiResponseService; HUMANO para las que el dueño
 * manda a mano desde el panel (modo híbrido, doc secciones 2 y 4 - ver
 * ConversationController.responder).
 */
public enum MessageSender {
    CLIENTE,
    BOT,
    HUMANO
}
