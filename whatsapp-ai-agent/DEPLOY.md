# Deploy en Railway

Dos servicios separados en el mismo proyecto de Railway, apuntando ambos a este mismo repo (monorepo), cada uno con su propio "Root Directory":

| Servicio  | Root Directory   | Dockerfile               | Puerto interno |
|-----------|-------------------|---------------------------|----------------|
| Backend   | `/`               | `Dockerfile`               | `PORT` (Railway lo inyecta, `application.properties` ya lo respeta) |
| Frontend  | `/panel-frontend` | `panel-frontend/Dockerfile`| `PORT` (idem, `server.js` de Next standalone lo respeta) |
| Postgres  | (plugin de Railway, no un servicio del repo) | - | - |

Railway detecta el `Dockerfile` de cada carpeta automáticamente (también hay un `railway.json` en cada una fijando el healthcheck). No hace falta tocar nada de esos dos archivos salvo que cambie el puerto o la ruta de salud.

## 1. Crear el proyecto

1. Crear cuenta en [railway.com](https://railway.com) (tiene un trial con crédito gratis, después es pago por uso - para este tamaño de proyecto sale unos pocos dólares al mes).
2. "New Project" → "Deploy from GitHub repo" → elegir este repo (hay que subirlo a GitHub primero si todavía no está — `git add mvnw mvnw.cmd .mvn Dockerfile panel-frontend/Dockerfile railway.json panel-frontend/railway.json .dockerignore panel-frontend/.dockerignore` y commitear todo lo de hoy).
3. Agregar el plugin de Postgres ("New" → "Database" → "PostgreSQL") dentro del mismo proyecto. Railway genera automáticamente las variables `DATABASE_URL`, `PGUSER`, `PGPASSWORD`, etc. — hay que mapearlas a las que usa este backend (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), ver checklist abajo.
4. Agregar el servicio del backend apuntando a este repo con Root Directory `/`.
5. Agregar el servicio del frontend apuntando a este mismo repo con Root Directory `/panel-frontend`.

## 2. Variables de entorno — Backend

Cargar en el servicio backend, pestaña "Variables":

**Obligatorias (el backend no arranca sin esto):**

| Variable | De dónde sale |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>:<puerto>/<db>` — armar a partir de lo que da el plugin de Postgres de Railway (o usar `${{Postgres.DATABASE_URL}}` como referencia de variable de Railway y ajustar el prefijo `jdbc:` a mano) |
| `DB_USERNAME` | Del plugin de Postgres |
| `DB_PASSWORD` | Del plugin de Postgres |
| `TWILIO_ACCOUNT_SID` | Consola de Twilio |
| `TWILIO_AUTH_TOKEN` | Consola de Twilio |
| `TWILIO_WHATSAPP_NUMBER` | El número de WhatsApp dedicado (doc sección 5.6) |
| `ANTHROPIC_API_KEY` | Consola de Anthropic |
| `JWT_SECRET` | Generar con `openssl rand -base64 32` (no reusar la de local) |
| `PANEL_USERNAME` | El que quieras para el login admin |
| `PANEL_PASSWORD` | Contraseña fuerte, no la de prueba local |

**Recomendadas para que todo funcione bien en producción:**

| Variable | Valor |
|---|---|
| `APP_BASE_URL` | La URL pública que Railway le asigna al backend (ej. `https://whatsapp-ai-agent-production.up.railway.app`) — la necesita PaymentService para armar los links de retorno de Flow |
| `PANEL_ALLOWED_ORIGINS` | La URL pública del frontend en Railway (ej. `https://panel-production.up.railway.app`) — si no se carga, CORS solo deja pasar `localhost:3000` y el panel desplegado no va a poder llamar a la API |
| `FLOW_API_BASE_URL` | `https://www.flow.cl/api` cuando se deje de usar el sandbox de Flow |

**Opcionales (features que se activan cuando estén cargadas, no rompen el arranque si faltan):**

`FLOW_BILLING_API_KEY`, `FLOW_BILLING_SECRET_KEY` (suscripción SaaS automática), `META_APP_SECRET`, `META_WEBHOOK_VERIFY_TOKEN` (Instagram), `INSTAGRAM_GRAPH_API_BASE_URL`, `REMINDERS_CONTENT_SID`, `REMINDERS_CONTENT_LANGUAGE` (plantilla WhatsApp aprobada).

`PORT` la inyecta Railway solo, no hace falta cargarla a mano.

## 3. Variables de entorno — Frontend

| Variable | Valor |
|---|---|
| `NEXT_PUBLIC_API_URL` | La URL pública del backend en Railway. **Importante**: Next.js graba esta variable adentro del bundle en el momento del *build*, no la lee en runtime — si se cambia después de desplegar, hay que forzar un rebuild (no alcanza con reiniciar el contenedor). Railway pasa las variables del servicio como build args automáticamente para builds con Dockerfile, así que cargarla acá alcanza. |

## 4. Orden recomendado la primera vez

1. Desplegar el backend primero, esperar que el healthcheck (`/actuator/health`) pase en verde y copiar su URL pública.
2. Cargar `NEXT_PUBLIC_API_URL` en el frontend con esa URL y recién ahí desplegarlo.
3. Copiar la URL pública del frontend y cargarla como `PANEL_ALLOWED_ORIGINS` en el backend (esto sí es una variable de runtime, no de build — con reiniciar el servicio alcanza).
4. Actualizar el webhook de Twilio (sandbox o número dedicado) para que apunte a `https://<url-backend>/webhooks/whatsapp`.
5. Si vas a usar Instagram: dar de alta el webhook en el Meta App Dashboard apuntando a `https://<url-backend>/webhooks/instagram`.

## 5. Qué falta después de esto (no cubierto por este documento)

- Dominio propio en vez del subdominio `*.up.railway.app` (Railway lo soporta, es un paso aparte en la pestaña "Settings" de cada servicio).
- Verificación de negocio ante Meta para levantar el límite de 2 números de WhatsApp sin SpA (doc sección 10).
- Backups de Postgres (Railway hace snapshots automáticos en los planes pagos, pero vale la pena confirmarlo antes de tener clientes reales con datos).
