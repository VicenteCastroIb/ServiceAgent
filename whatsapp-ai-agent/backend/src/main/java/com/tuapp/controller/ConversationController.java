package com.tuapp.controller;

import com.tuapp.model.ChannelType;
import com.tuapp.model.Conversation;
import com.tuapp.model.Message;
import com.tuapp.model.MessageDirection;
import com.tuapp.model.MessageSender;
import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.ConversationService;
import com.tuapp.service.InstagramMessagingService;
import com.tuapp.service.MessagingService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Bandeja de conversaciones del panel: modo híbrido real (doc secciones 2 y
 * 4). A diferencia de HandoffController (que solo lista/reanuda conversaciones
 * PAUSADAS), acá se ve el historial completo de cualquier conversación y se
 * puede responder a mano - necesario porque, una vez que un número queda
 * conectado a la WhatsApp Business API, ya no se puede usar desde una app de
 * WhatsApp normal: la única forma de responder como ese negocio es
 * programática (ver MessagingService), y este endpoint es el que la dispara
 * cuando quien "escribe" es un humano en vez del bot.
 * <p>
 * Autorización por tenant igual que el resto de /admin/**: el admin ve todas
 * las conversaciones de todos los negocios, el dueño de un negocio solo las
 * suyas (ver PanelAuth).
 */
@Slf4j
@RestController
@RequestMapping("/admin/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final TenantService tenantService;
    private final MessagingService messagingService;
    private final InstagramMessagingService instagramMessagingService;

    public ConversationController(
            ConversationService conversationService,
            TenantService tenantService,
            MessagingService messagingService,
            InstagramMessagingService instagramMessagingService) {
        this.conversationService = conversationService;
        this.tenantService = tenantService;
        this.messagingService = messagingService;
        this.instagramMessagingService = instagramMessagingService;
    }

    @GetMapping
    public List<ConversationView> listar() {
        Tenant tenant = PanelAuth.esAdmin() ? null : tenantService.buscarPorId(PanelAuth.tenantIdActual()).orElse(null);
        return conversationService.listar(tenant).stream().map(ConversationView::de).toList();
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<MessageView>> listarMensajes(@PathVariable Long id) {
        Conversation conversation = conversationService.buscarPorId(id).orElse(null);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        if (!PanelAuth.puedeAcceder(conversation.getTenant().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<MessageView> mensajes = conversationService.listarMensajes(conversation).stream()
                .map(MessageView::de)
                .toList();
        return ResponseEntity.ok(mensajes);
    }

    /**
     * Manda una respuesta manual del dueño al cliente (modo híbrido). No
     * reanuda automáticamente el bot si la conversación estaba pausada por un
     * handoff - eso sigue siendo una acción aparte (ver HandoffController),
     * para que el dueño decida cuándo devolverle el control al bot en vez de
     * que se reactive solo apenas contesta una vez.
     */
    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responder(@PathVariable Long id, @Valid @RequestBody ResponderRequest request) {
        Conversation conversation = conversationService.buscarPorId(id).orElse(null);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        Tenant tenant = conversation.getTenant();
        if (!PanelAuth.puedeAcceder(tenant.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            enviarPorCanal(tenant, conversation, request.texto());
        } catch (Exception e) {
            log.error("No se pudo enviar la respuesta manual de la conversación {} (tenant {})", id, tenant.getId(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("No se pudo enviar el mensaje. Probá de nuevo en unos minutos."));
        }

        Message guardado = conversationService.registrarMensaje(
                conversation, MessageDirection.OUT, MessageSender.HUMANO, request.texto());
        return ResponseEntity.ok(MessageView.de(guardado));
    }

    private void enviarPorCanal(Tenant tenant, Conversation conversation, String texto) {
        if (conversation.getChannel() == ChannelType.INSTAGRAM) {
            String igsid = conversation.getClientContact().replaceFirst("^instagram:", "");
            instagramMessagingService.enviarMensaje(tenant, igsid, texto);
        } else {
            messagingService.enviarWhatsApp(conversation.getClientContact(), texto);
        }
    }

    public record ConversationView(
            Long id, ChannelType channel, String clientContact, Instant lastMessageAt, Long tenantId, String businessName) {
        static ConversationView de(Conversation c) {
            return new ConversationView(
                    c.getId(), c.getChannel(), c.getClientContact(), c.getLastMessageAt(),
                    c.getTenant().getId(), c.getTenant().getBusinessName());
        }
    }

    public record MessageView(Long id, MessageDirection direction, MessageSender sender, String content, Instant sentAt) {
        static MessageView de(Message m) {
            return new MessageView(m.getId(), m.getDirection(), m.getSender(), m.getContent(), m.getSentAt());
        }
    }

    public record ResponderRequest(@NotBlank String texto) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
