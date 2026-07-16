package com.tuapp.service;

import com.tuapp.model.ChannelType;
import com.tuapp.model.Conversation;
import com.tuapp.model.Message;
import com.tuapp.model.MessageDirection;
import com.tuapp.model.MessageSender;
import com.tuapp.model.Tenant;
import com.tuapp.repository.ConversationRepository;
import com.tuapp.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de ConversationService: find-or-create de la Conversation y
 * registro de mensajes, que es la base de la bandeja del panel (doc sección
 * 2 - "modo híbrido"). Repositorios mockeados, sin base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;

    private ConversationService conversationService;

    private Tenant tenant() {
        Tenant t = new Tenant();
        t.setId(1L);
        t.setBusinessName("Cafetería Don José");
        return t;
    }

    @Test
    void obtenerOCrear_devuelveLaExistenteSiYaHay() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
        Tenant tenant = tenant();
        Conversation existente = new Conversation();
        existente.setId(5L);
        when(conversationRepository.findByTenantAndClientContact(tenant, "whatsapp:+56911111111"))
                .thenReturn(Optional.of(existente));

        Conversation resultado = conversationService.obtenerOCrear(tenant, ChannelType.WHATSAPP, "whatsapp:+56911111111");

        assertThat(resultado).isEqualTo(existente);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void obtenerOCrear_creaUnaNuevaSiNoExiste() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
        Tenant tenant = tenant();
        when(conversationRepository.findByTenantAndClientContact(tenant, "whatsapp:+56922222222"))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        Conversation resultado = conversationService.obtenerOCrear(tenant, ChannelType.WHATSAPP, "whatsapp:+56922222222");

        assertThat(resultado.getTenant()).isEqualTo(tenant);
        assertThat(resultado.getChannel()).isEqualTo(ChannelType.WHATSAPP);
        assertThat(resultado.getClientContact()).isEqualTo("whatsapp:+56922222222");
        assertThat(resultado.getCreatedAt()).isNotNull();
    }

    @Test
    void registrarMensaje_guardaElMensajeYActualizaLastMessageAt() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        Message resultado = conversationService.registrarMensaje(
                conversation, MessageDirection.IN, MessageSender.CLIENTE, "hola, tienen mesa?");

        assertThat(resultado.getDirection()).isEqualTo(MessageDirection.IN);
        assertThat(resultado.getSender()).isEqualTo(MessageSender.CLIENTE);
        assertThat(resultado.getContent()).isEqualTo("hola, tienen mesa?");
        assertThat(resultado.getSentAt()).isNotNull();
        assertThat(conversation.getLastMessageAt()).isEqualTo(resultado.getSentAt());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void listar_conTenantUsaLaBandejaDeEseTenant() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
        Tenant tenant = tenant();
        List<Conversation> esperado = List.of(new Conversation());
        when(conversationRepository.findByTenantOrderByLastMessageAtDesc(tenant)).thenReturn(esperado);

        List<Conversation> resultado = conversationService.listar(tenant);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void listar_sinTenantUsaLaBandejaGlobal() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
        List<Conversation> esperado = List.of(new Conversation(), new Conversation());
        when(conversationRepository.findAllByOrderByLastMessageAtDesc()).thenReturn(esperado);

        List<Conversation> resultado = conversationService.listar(null);

        assertThat(resultado).isEqualTo(esperado);
    }
}
