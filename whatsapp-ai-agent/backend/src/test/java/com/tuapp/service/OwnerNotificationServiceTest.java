package com.tuapp.service;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests de OwnerNotificationService.notificarNuevoTenantActivo (doc sección
 * 12): el aviso al ADMIN cuando un tenant self-service confirma su primer
 * pago. Cubre los casos "apagado" (sin SMTP / sin platform.admin-email) -
 * nunca debe tirar excepción ni mandar nada - y el caso feliz.
 */
@ExtendWith(MockitoExtension.class)
class OwnerNotificationServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private JavaMailSender mailSender;

    private Tenant tenant() {
        Tenant t = new Tenant();
        t.setId(1L);
        t.setBusinessName("Cafetería Don José");
        t.setPlan(TenantPlan.PRO);
        t.setOwnerEmail("jose@cafeteria.cl");
        return t;
    }

    @Test
    void notificarNuevoTenantActivo_noMandaNadaSiNoHayServidorSmtpConfigurado() {
        OwnerNotificationService service = new OwnerNotificationService(
                tenantRepository, mailSender, "no-reply@app.cl", "https://panel.app.cl",
                "admin@app.cl", /* mailHost */ "");

        service.notificarNuevoTenantActivo(tenant());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notificarNuevoTenantActivo_noMandaNadaSiNoHayEmailDeAdminConfigurado() {
        OwnerNotificationService service = new OwnerNotificationService(
                tenantRepository, mailSender, "no-reply@app.cl", "https://panel.app.cl",
                /* adminEmail */ "", "smtp.gmail.com");

        service.notificarNuevoTenantActivo(tenant());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notificarNuevoTenantActivo_mandaElEmailAlAdminCuandoTodoEstaConfigurado() {
        OwnerNotificationService service = new OwnerNotificationService(
                tenantRepository, mailSender, "no-reply@app.cl", "https://panel.app.cl",
                "admin@app.cl", "smtp.gmail.com");

        service.notificarNuevoTenantActivo(tenant());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void notificarNuevoTenantActivo_nuncaTiraExcepcionSiFallaElEnvio() {
        OwnerNotificationService service = new OwnerNotificationService(
                tenantRepository, mailSender, "no-reply@app.cl", "https://panel.app.cl",
                "admin@app.cl", "smtp.gmail.com");
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp caído"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // No debe propagar la excepción - best effort (ver Javadoc de la clase).
        service.notificarNuevoTenantActivo(tenant());
    }
}
