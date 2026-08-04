# ServiceAgent

Agente de IA para WhatsApp e Instagram: atiende automáticamente los mensajes de tiendas y locales pequeños (catálogo, precios, horarios, tono propio), agenda citas y deriva a un humano cuando hace falta.

## Estructura

```
whatsapp-ai-agent/
  backend/            Spring Boot + Postgres (API, webhooks, lógica de IA)
  frontend/landing/    Next.js — sitio de marketing (ver DEPLOY.md)
  frontend/panel/      Next.js — panel de administración por tenant
  tools/                Scripts auxiliares (carga de catálogo de prueba, etc.)
docs/
  economia-unit-economics.md   Costos y márgenes por plan
  design-handoffs/             Mockups y capturas de referencia de diseño
```

## Desarrollo

Cada servicio se corre por separado — ver el README/comentarios de cada carpeta para el detalle. En resumen:

```bash
# Backend (requiere Postgres via docker-compose, o perfil "local" con H2)
cd whatsapp-ai-agent/backend && ./mvnw spring-boot:run

# Panel de administración (puerto 3000)
cd whatsapp-ai-agent/frontend/panel && npm run dev

# Landing de marketing (puerto 3100)
cd whatsapp-ai-agent/frontend/landing && npm run dev
```

## Ramas

`main` (producción), `test`, `dev` (donde se desarrolla). Todos los cambios se aplican sobre `dev`.
