package com.tuapp.service;

import com.tuapp.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Refresca los tokens de Instagram de los tenants antes de que venzan (son
 * de larga duración, pero vencen a los 60 días - ver InstagramMessagingService
 * y doc secciones 3 y 5.1). Corre una vez al día y refresca los que vencen
 * dentro de los próximos N días (por defecto 10, con margen de sobra).
 */
@Slf4j
@Component
public class InstagramTokenRefreshJob {

    private final TenantService tenantService;
    private final InstagramMessagingService instagramMessagingService;
    private final long anticipacionDias;

    public InstagramTokenRefreshJob(
            TenantService tenantService,
            InstagramMessagingService instagramMessagingService,
            @Value("${instagram.refresh-anticipacion-dias:10}") long anticipacionDias) {
        this.tenantService = tenantService;
        this.instagramMessagingService = instagramMessagingService;
        this.anticipacionDias = anticipacionDias;
    }

    @Scheduled(fixedDelayString = "${instagram.refresh-intervalo-ms:86400000}")
    public void refrescarTokensPorVencer() {
        Instant limite = Instant.now().plus(Duration.ofDays(anticipacionDias));
        List<Tenant> tenants = tenantService.listarConTokenInstagramPorVencer(limite);

        for (Tenant tenant : tenants) {
            try {
                instagramMessagingService.refrescarToken(tenant);
            } catch (Exception e) {
                // Un tenant con problemas de token (revocado, etc.) no debe
                // frenar el refresh de los demás.
                log.error("Falló el refresh de token de Instagram para tenant {}: {}", tenant.getId(), e.getMessage(), e);
            }
        }

        if (!tenants.isEmpty()) {
            log.info("Tokens de Instagram refrescados: {}", tenants.size());
        }
    }
}
