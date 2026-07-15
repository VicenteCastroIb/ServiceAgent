package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Mensaje individual dentro de una Conversation (entrante o saliente).
 *
 * TODO: dirección (in/out), autor (cliente/bot/humano), contenido, timestamps.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant sentAt;
}
