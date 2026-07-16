package com.tuapp.model;

/**
 * Canal de una Conversation (doc secciones 2, 3 y 5.1). Se resuelve al crear
 * la conversación según de dónde vino el mensaje (WebhookController vs
 * InstagramWebhookController) - no se infiere del formato de clientContact en
 * ningún otro lado, para no depender de ese detalle interno.
 */
public enum ChannelType {
    WHATSAPP,
    INSTAGRAM
}
