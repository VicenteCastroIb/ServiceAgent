package com.tuapp.controller;

import com.tuapp.service.HandoffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración de conversaciones derivadas a humano ("modo
 * híbrido", ver doc secciones 2 y 4). La consume el panel Next.js para que
 * el dueño vea qué clientes esperan atención manual y reactive el bot cuando
 * ya los atendió.
 *
 * Protegida por JWT igual que {@link TenantController} (ver SecurityConfig).
 */
@RestController
@RequestMapping("/admin/handoffs")
public class HandoffController {

    private final HandoffService handoffService;

    public HandoffController(HandoffService handoffService) {
        this.handoffService = handoffService;
    }

    @GetMapping
    public List<HandoffDto> listar() {
        return handoffService.listarPausadas().entrySet().stream()
                .map(entrada -> new HandoffDto(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    @PostMapping("/{numeroCliente}/reanudar")
    public ResponseEntity<Void> reanudar(@PathVariable String numeroCliente) {
        handoffService.reanudar(numeroCliente);
        return ResponseEntity.noContent().build();
    }

    public record HandoffDto(String numeroCliente, String motivo) {
    }
}
