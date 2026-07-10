# whatsapp-ai-agent

Backend del agente de IA para WhatsApp e Instagram (ver plan de proyecto v2 en la raíz del workspace).

## Stack
Java 21 · Spring Boot 3.3.2 · Maven · Postgres · Thymeleaf (panel) · Spring Security

## Requisitos
- JDK 21+
- Maven 3.9+
- Docker (para Postgres local)

## Levantar en local

```bash
# 1. Levantar Postgres
docker compose up -d

# 2. Correr la app
mvn spring-boot:run
```

La app queda en `http://localhost:8080`.

## Estructura (doc, sección 5.2)

```
src/main/java/com/tuapp/
  controller/   WebhookController        - webhooks de WhatsApp (Twilio) e Instagram
  service/      AiResponseService, MessagingService, HandoffService,
                TenantService, SchedulingService, PaymentService, CatalogSyncService
  model/        Tenant, Conversation, Product, Message, Appointment, Availability
  repository/   interfaces JpaRepository por cada modelo
  config/       SecurityConfig
```

Todas las clases de `service/` y `controller/` son stubs por ahora (Semana 1 del roadmap:
setup + sandbox Twilio + webhook básico). La lógica se agrega semana a semana según el roadmap del doc.

## Variables pendientes de configurar
- `twilio.account-sid`, `twilio.auth-token`, `twilio.whatsapp-number` en `application.properties` (o como env vars) una vez creada la cuenta sandbox de Twilio.
