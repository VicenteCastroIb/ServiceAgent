package com.tuapp.controller;

import com.tuapp.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhooks propios de Flow (plan Catálogo, ver PaymentService y doc secciones
 * 3, 5.1 y 5.3). Público a nivel de Spring Security (ver SecurityConfig,
 * /webhooks/** permitAll) igual que el webhook de Twilio - acá no hay firma
 * que validar como con Twilio, así que la "autenticación" real es que nunca
 * confiamos en el estado que venga en el request: solo usamos el token para
 * encontrar la orden y consultamos el estado real llamando de vuelta a Flow
 * con nuestras propias credenciales firmadas (ver PaymentService.procesarConfirmacion).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/flow")
public class FlowWebhookController {

    private final PaymentService paymentService;

    public FlowWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Flow llama acá (parámetro urlConfirmation de payment/create) tras un
     * intento de pago. Hay que responder 200 en menos de 15 segundos - ver
     * PaymentService.procesarConfirmacion, que nunca propaga errores hacia
     * afuera para no comprometer ese tiempo de respuesta.
     */
    @PostMapping("/confirmacion/{tenantId}")
    public ResponseEntity<Void> confirmacion(@PathVariable Long tenantId, @RequestParam String token) {
        paymentService.procesarConfirmacion(tenantId, token);
        return ResponseEntity.ok().build();
    }

    /**
     * Página de retorno genérica (parámetro urlReturn) para cuando el tenant
     * no cargó su propia tienda/URL de retorno - ver
     * PaymentService.generarLinkPago. Flow redirige acá el navegador del
     * cliente tras el pago; como el cliente llegó por WhatsApp no hay sesión
     * web real que retomar, solo se le pide que vuelva al chat.
     */
    @GetMapping("/retorno")
    public ResponseEntity<String> retorno() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<p>Gracias. Ya podés volver a WhatsApp para ver la confirmación de tu compra.</p>");
    }
}
