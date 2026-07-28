# Deploy gratis: Supabase + Render + Vercel

Stack para la etapa de pruebas/piloto, sin costo mensual:

| Pieza | Dónde | Plan | Costo |
|---|---|---|---|
| Base de datos (Postgres) | [Supabase](https://supabase.com) | Free | $0 (500MB, se pausa tras 1 semana sin uso) |
| Backend (Spring Boot) | [Render](https://render.com) | Free | $0 (duerme a los 15 min sin tráfico - ver sección 3, cron de keepalive) |
| Panel (Next.js) | [Vercel](https://vercel.com) | Hobby | $0 (**no comercial** - ver nota abajo) |
| Landing (Next.js) | [Vercel](https://vercel.com) | Hobby | $0 (idem) |

**Sobre "no comercial" en Vercel Hobby**: el plan gratuito de Vercel prohíbe explícitamente proyectos que generen ingresos. Sirve perfecto para la etapa de pruebas/piloto de ahora (sin cobrarle a nadie todavía), pero antes de vender el producto en serio hay que pasar panel/landing a Vercel Pro (~USD 20/mes) o migrar a una alternativa gratuita que sí permite uso comercial, como **Cloudflare Pages** (requiere adaptar el build con el adapter de Next.js para Cloudflare - no es zero-config como Vercel, evaluarlo cuando llegue el momento). Anotalo como pendiente antes de la primera venta real.

Cuando el tráfico crezca y valga la pena pagar por evitar cold starts/límites de conexión, ver [`DEPLOY_RAILWAY.md`](./DEPLOY_RAILWAY.md) como alternativa todo-en-uno.

## 0. Antes de empezar

Subí a GitHub todo lo de esta ronda de cambios si todavía no lo hiciste (ver los mensajes de commit sugeridos en la conversación). Vas a necesitar el repo en GitHub para conectar Render y Vercel.

## 1. Base de datos — Supabase

1. Crear cuenta en [supabase.com](https://supabase.com) → "New Project". Elegí una región cercana a donde vas a desplegar el backend (Render tiene regiones en EE.UU. y Europa - Oregon suele ser la más cercana a Chile disponible en el free tier).
2. Anotá la contraseña del proyecto que te pide al crearlo (es la de Postgres) - no queda visible después, solo se puede resetear.
3. **Connection string — usar el Session Pooler, no la conexión directa ni el Transaction Pooler**: en el dashboard, "Connect" → pestaña "Session pooler". Este backend usa un pool de conexiones persistente normal (HikariCP + Hibernate con prepared statements) - el Transaction Pooler (puerto 6543) puede romper con prepared statements, y la conexión directa (puerto 5432 sin pooler) requiere IPv6, que Render no siempre soporta en el free tier. El Session Pooler da compatibilidad IPv4 completa sin esas limitaciones.
4. Con esos datos, armá las 3 variables que pide el backend:
   - `DB_URL=jdbc:postgresql://<host-del-pooler>:5432/postgres?sslmode=require`
   - `DB_USERNAME=postgres.<referencia-de-tu-proyecto>` (así viene formado en el Session Pooler, no es solo "postgres")
   - `DB_PASSWORD=<la contraseña del paso 2>`

### Seguridad de Supabase

- **SSL obligatorio**: Database → Settings → SSL Configuration → activar "Enforce SSL on incoming connections". El `?sslmode=require` de arriba ya fuerza SSL del lado del cliente; esto lo fuerza también del lado del servidor.
- **Network Restrictions (IP allow-list)**: Database → Network Restrictions, si tu plan lo incluye - restringí las conexiones a las IPs salientes de Render (Render las muestra en el dashboard del servicio, "Outbound IP Addresses"; si Render usa IPs dinámicas en el free tier, este paso puede no ser viable ahí - confirmalo en el dashboard antes de bloquear todo por error).
- **Contraseña fuerte y rotación**: no reuses la contraseña de ningún otro servicio; si alguna vez sospechás que se filtró, se resetea desde el mismo dashboard (Settings → Database → Reset database password) - eso sí, hay que actualizar `DB_PASSWORD` en Render inmediatamente después o el backend deja de poder conectarse.
- El resto de las credenciales de la app (`JWT_SECRET`, `APP_ENCRYPTION_KEY`, `PANEL_PASSWORD`, etc.) siguen siendo las de siempre - no tienen nada que ver con Supabase, van directo en las variables de entorno de Render (sección 2).

## 2. Backend — Render

1. Crear cuenta en [render.com](https://render.com) (sin tarjeta) → "New" → "Web Service" → conectar el repo de GitHub.
2. Root Directory: `whatsapp-ai-agent/backend`. Render detecta el `Dockerfile` solo (Runtime: Docker).
3. Plan: Free.
4. Health Check Path: `/actuator/health`.
5. Variables de entorno (Environment → Add Environment Variable) - las mismas que ya usás en `.env` local, cargadas acá con los valores de producción:

**Obligatorias:**

| Variable | Valor |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Los del Session Pooler de Supabase (sección 1) |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_NUMBER` | Consola de Twilio (para pruebas, el sandbox alcanza - ver sección 6) |
| `ANTHROPIC_API_KEY` | Consola de Anthropic |
| `JWT_SECRET` | `openssl rand -base64 32` (generar una nueva, no reusar la de local) |
| `APP_ENCRYPTION_KEY` | `openssl rand -base64 32` (otra distinta de `JWT_SECRET`) |
| `PANEL_USERNAME`, `PANEL_PASSWORD` | Login del admin, contraseña fuerte |

**Recomendadas:**

| Variable | Valor |
|---|---|
| `APP_BASE_URL` | La URL pública que Render le asigna (ej. `https://whatsapp-ai-agent-backend.onrender.com`) |
| `PANEL_ALLOWED_ORIGINS` | La URL pública del panel en Vercel, una vez desplegado (sección 4) |
| `COOKIE_SAME_SITE` | Dejar sin cargar (default `None`) mientras panel/backend estén en dominios distintos - ver sección 5 |

**Opcionales** (mismo criterio que antes - activan features si están, no rompen el arranque si faltan): `FLOW_BILLING_API_KEY`/`FLOW_BILLING_SECRET_KEY`, `META_APP_SECRET`/`META_WEBHOOK_VERIFY_TOKEN`, `REMINDERS_CONTENT_SID`/`REMINDERS_CONTENT_LANGUAGE`, `MAIL_SMTP_*`, `PANEL_PUBLIC_URL`, `PLATFORM_ADMIN_EMAIL`.

6. Deploy. Cuando el healthcheck pase en verde, copiá la URL pública (`https://<nombre>.onrender.com`).

## 3. Que Render no se duerma (cron de keepalive)

El plan free de Render duerme el servicio a los 15 minutos sin tráfico entrante (cold start de 30-60s al despertar) - un cliente real escribiéndole al bot no puede esperar eso.

Ya está el workflow `.github/workflows/keep-render-awake.yml`, que pinguea el backend cada 10 minutos. Para activarlo:

1. En GitHub: Settings del repo → Secrets and variables → Actions → pestaña "Variables" → "New repository variable".
2. Nombre: `RENDER_HEALTHCHECK_URL`. Valor: `https://<tu-backend>.onrender.com/actuator/health`.
3. Listo - corre solo cada 10 minutos.

**Respaldo recomendado**: los cron de GitHub Actions no son en tiempo real (pueden demorarse en repos con poca actividad). Como segunda red de seguridad, agregá gratis un monitor en [cron-job.org](https://cron-job.org) o [UptimeRobot](https://uptimerobot.com) apuntando a la misma URL cada 5-10 minutos - son gratis y más confiables para este propósito puntual.

## 4. Panel y landing — Vercel

1. Crear cuenta en [vercel.com](https://vercel.com) → "Add New" → "Project" → importar el repo de GitHub.
2. **Panel**: Root Directory `whatsapp-ai-agent/frontend/panel`. Vercel detecta Next.js solo (no usa el `Dockerfile`, que es para Render/Railway - Vercel tiene su propio builder). Variables de entorno:
   - `NEXT_PUBLIC_API_URL` = la URL del backend en Render.
3. Deploy, copiar la URL pública del panel (`https://<algo>.vercel.app`).
4. Cargar esa URL como `PANEL_ALLOWED_ORIGINS` en Render (variable de runtime - con que Render reinicie el servicio alcanza, no hace falta rebuild).
5. **Landing**: importar el mismo repo de nuevo como otro proyecto de Vercel, Root Directory `whatsapp-ai-agent/frontend/landing`. Variables de entorno:
   - `NEXT_PUBLIC_API_URL` = la URL del backend en Render.
   - `NEXT_PUBLIC_WHATSAPP_SALES_NUMBER` = tu número de ventas.
   - `NEXT_PUBLIC_PANEL_URL` = la URL del panel (paso 3).
   - `NEXT_PUBLIC_SITE_URL` = la URL pública final de la landing (el propio dominio `.vercel.app` mientras no tengas uno propio).
   - `NEXT_PUBLIC_GA_ID` / `NEXT_PUBLIC_META_PIXEL_ID` = opcionales, dejar vacías mientras no crees esas cuentas.
6. Deploy.

**Importante sobre `NEXT_PUBLIC_*`**: Next.js las hornea en el build, no las lee en runtime. Si cambiás una después de desplegar, hay que forzar un "Redeploy" en Vercel (no alcanza con que el servicio siga corriendo).

## 5. Orden recomendado de principio a fin

1. Supabase (sección 1) → copiar credenciales del Session Pooler.
2. Render backend (sección 2) → esperar healthcheck verde → copiar URL pública.
3. Activar el cron de keepalive (sección 3).
4. Vercel panel (sección 4) → cargar `NEXT_PUBLIC_API_URL` → deploy → copiar URL → cargarla como `PANEL_ALLOWED_ORIGINS` en Render.
5. Vercel landing (sección 4) → cargar el resto de variables → deploy.
6. Activar el sandbox de Twilio y apuntar el webhook al backend de Render (sección 6) → empezar a probar.

## 6. Twilio: probar ya con el sandbox (gratis, sin comprar número)

No hace falta comprar un número dedicado para las pruebas internas:

1. En la [consola de Twilio](https://console.twilio.com), ir a Messaging → Try it out → Send a WhatsApp message, para ver el código del sandbox (algo como "join palabra-clave") y el número del sandbox (`+14155238886`, ya está en `TWILIO_WHATSAPP_NUMBER`).
2. Cualquiera que quiera probar manda ese "join <código>" desde su WhatsApp personal al número del sandbox - queda habilitado para chatear con el bot.
3. En la consola, "Sandbox Settings" → "WHEN A MESSAGE COMES IN" → pegar `https://<tu-backend>.onrender.com/webhooks/whatsapp`, método POST.
4. Listo - los mensajes que le escribas al sandbox desde tu WhatsApp ya le llegan al backend desplegado.

Cuando quieras pasar a un número dedicado real (para vender, no solo para probar), comprarlo desde la consola de Twilio y pedir la verificación de WhatsApp Business del número - eso ya implica costo mensual del número + por mensaje (ver conversación previa sobre precios).

## 7. Dominio propio y la cookie de login (`SameSite`)

Mientras no haya dominio propio, backend (Render) y panel/landing (Vercel) quedan en dominios completamente distintos - eso es "cross-site" para cookies. El JWT del panel viaja en una cookie httpOnly (ver `AuthController`) emitida con `SameSite=None` por default (`COOKIE_SAME_SITE`, ver `application.properties`) para que al menos tenga chance de funcionar cross-site: Chrome la deja pasar, pero Safari/Firefox pueden bloquearla igual por sus reglas de cookies de terceros - en el peor caso, un dueño de negocio no puede loguearse en ciertos navegadores.

Apenas tengas dominio propio:

1. Asignar subdominios bajo el mismo dominio raíz: `tudominio.cl` (landing, en Vercel), `panel.tudominio.cl` (panel, en Vercel), `api.tudominio.cl` (backend, en Render) - cada plataforma tiene su propia sección de "Custom Domains".
2. Cargar `COOKIE_SAME_SITE=Lax` en Render.
3. Actualizar `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_PANEL_URL`, `NEXT_PUBLIC_SITE_URL` y `PANEL_ALLOWED_ORIGINS` con las URLs nuevas, y forzar un redeploy de panel y landing.
4. Actualizar el webhook de Twilio (y el de Meta si usás Instagram) con la nueva URL de `api.tudominio.cl`.

## 8. Qué falta después de esto

- Pasar Vercel a plan Pro (o migrar a Cloudflare Pages) antes de la primera venta real - ver nota del principio.
- Verificación de negocio ante Meta para levantar el límite de 2 números de WhatsApp sin SpA (doc del proyecto, sección 10).
- Backups de la base: en el free tier de Supabase no hay backups automáticos - exportar manualmente de vez en cuando (`pg_dump` contra el connection string) hasta que valga la pena pasar a un plan pago con backups incluidos.
- CI/CD: `.github/workflows/ci.yml` corre tests del backend y build/lint de panel y landing en cada push/PR, pero no despliega solo - Render y Vercel ya redespliegan automático en cada push a la rama conectada, así que no hace falta nada más ahí.
