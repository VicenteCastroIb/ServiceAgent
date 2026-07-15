package com.tuapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter simple en memoria, por clave (ej. "registro:" + ip), ventana
 * fija. Pensado para frenar abuso básico en endpoints públicos sin sesión
 * (ej. RegistroController, que dispara llamadas a la API de Flow por cada
 * intento) - no es una defensa completa contra un atacante que rota de IP,
 * pero sí frena el caso simple de spam/loop desde un mismo origen.
 *
 * OJO: en memoria por proceso - sirve mientras la app corre en una sola
 * instancia (Railway/Render, doc sección 5.1). Si en el futuro se escala a
 * múltiples instancias detrás de un load balancer, esto hay que moverlo a un
 * store compartido (Redis), porque cada instancia tendría su propio contador
 * y el límite real efectivo se multiplicaría por la cantidad de instancias.
 */
@Slf4j
@Component
public class RateLimiter {

    private record Ventana(Instant inicio, AtomicInteger conteo) {
    }

    private final Map<String, Ventana> intentos = new ConcurrentHashMap<>();

    /**
     * Registra un intento para {@code clave} y devuelve true si todavía hay
     * cupo dentro de la ventana (consume uno). false si se superó
     * {@code maxIntentos} intentos dentro de {@code ventana}.
     */
    public boolean permitir(String clave, int maxIntentos, Duration ventana) {
        Instant ahora = Instant.now();
        Ventana actual = intentos.compute(clave, (k, v) -> {
            if (v == null || Duration.between(v.inicio(), ahora).compareTo(ventana) > 0) {
                return new Ventana(ahora, new AtomicInteger(1));
            }
            v.conteo().incrementAndGet();
            return v;
        });
        return actual.conteo().get() <= maxIntentos;
    }

    /**
     * Limpieza periódica para no acumular memoria indefinidamente con IPs que
     * ya no vuelven a pegarle al endpoint. Corre cada hora y borra ventanas
     * con más de 2 horas de antigüedad (margen amplio: ninguna ventana usada
     * hoy dura más de 1 hora).
     */
    @Scheduled(fixedDelay = 3_600_000)
    void limpiarVentanasVencidas() {
        Instant limite = Instant.now().minus(Duration.ofHours(2));
        int antes = intentos.size();
        intentos.entrySet().removeIf(e -> e.getValue().inicio().isBefore(limite));
        int borradas = antes - intentos.size();
        if (borradas > 0) {
            log.debug("RateLimiter: limpiadas {} ventanas vencidas", borradas);
        }
    }
}
