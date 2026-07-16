package com.tuapp.service;

import com.tuapp.model.ChannelType;
import com.tuapp.model.Conversation;
import com.tuapp.model.Message;
import com.tuapp.model.MessageDirection;
import com.tuapp.model.MessageSender;
import com.tuapp.model.Tenant;
import com.tuapp.repository.ConversationRepository;
import com.tuapp.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persiste el historial de conversaciones/mensajes para la bandeja del panel
 * (doc sección 2: "puede ver y responder los chats ahí mismo"). Lo consumen
 * los webhooks (WebhookController/InstagramWebhookController) para cada
 * mensaje entrante/saliente, y ConversationController para que el panel lea
 * ese historial y mande respuestas manuales.
 * <p>
 * A propósito NO toca el estado de pausa por handoff (eso sigue en
 * HandoffService, en memoria) - esta clase solo guarda texto e historial,
 * nunca decide si el bot debe responder o no.
 * <p>
 * registrarMensaje() es best-effort desde el punto de vista de los webhooks:
 * un fallo acá (ver los catch en los callers) nunca debe frenar la respuesta
 * al cliente - peor caso, ese mensaje puntual no queda en el historial del
 * panel, pero WhatsApp/Instagram le siguen contestando normal.
 */
@Slf4j
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation obtenerOCrear(Tenant tenant, ChannelType channel, String clientContact) {
        return conversationRepository.findByTenantAndClientContact(tenant, clientContact)
                .orElseGet(() -> {
                    Conversation nueva = new Conversation();
                    nueva.setTenant(tenant);
                    nueva.setChannel(channel);
                    nueva.setClientContact(clientContact);
                    nueva.setCreatedAt(Instant.now());
                    return conversationRepository.save(nueva);
                });
    }

    @Transactional
    public Message registrarMensaje(Conversation conversation, MessageDirection direction, MessageSender sender, String content) {
        Instant ahora = Instant.now();

        Message mensaje = new Message();
        mensaje.setConversation(conversation);
        mensaje.setDirection(direction);
        mensaje.setSender(sender);
        mensaje.setContent(content);
        mensaje.setSentAt(ahora);
        Message guardado = messageRepository.save(mensaje);

        conversation.setLastMessageAt(ahora);
        conversationRepository.save(conversation);

        return guardado;
    }

    /**
     * Registra un mensaje entrante/saliente resolviendo (o creando) la
     * Conversation de una - atajo para los webhooks, que no necesitan tocar
     * Conversation directamente.
     */
    @Transactional
    public void registrarMensaje(
            Tenant tenant, ChannelType channel, String clientContact,
            MessageDirection direction, MessageSender sender, String content) {
        Conversation conversation = obtenerOCrear(tenant, channel, clientContact);
        registrarMensaje(conversation, direction, sender, content);
    }

    /** Bandeja para el panel: todas las de un tenant, o todas si tenantId es null (admin). */
    public List<Conversation> listar(Tenant tenantONull) {
        return tenantONull != null
                ? conversationRepository.findByTenantOrderByLastMessageAtDesc(tenantONull)
                : conversationRepository.findAllByOrderByLastMessageAtDesc();
    }

    public Optional<Conversation> buscarPorId(Long id) {
        return conversationRepository.findById(id);
    }

    public List<Message> listarMensajes(Conversation conversation) {
        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }
}
