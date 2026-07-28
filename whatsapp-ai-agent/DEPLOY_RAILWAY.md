# Deploy en Railway (alternativa paga)

> Esta es la guía original de deploy en Railway. Se mantiene como alternativa
> por si en algún momento conviene volver (por ejemplo, si el volumen de
> tráfico hace que valga la pena pagar por no tener cold starts ni límites de
> conexión). **La guía principal ahora es [`DEPLOY.md`](./DEPLOY.md)**, con
> Render + Vercel + Supabase, que cubre el mismo despliegue sin costo mensual
> mientras el proyecto está en etapa de pruebas/piloto.

El repo está organizado como monorepo: `backend/` (Spring Boot), `frontend/panel/` (panel de administración, Next.js) y `frontend/landing/` (sitio de marketing/registro self-service, Next.js). Tres servicios separados en el mismo proyecto de Railway, apuntando todos a este mismo repo, cada uno con su propio "Root Directory":

| Servicio  | Root Directory   | Dockerfile               | Puerto interno |
|-----------|-------------------|---------------------------|----------------|
| Backend   | `/backend`               | `Dockerfile`               | `PORT` (Railway lo inyecta, `application.properties` ya lo respeta) |
| Frontend (panel) | `/frontend/panel` | `Dockerfile`| `PORT` (idem, `server.js` de Next standalone lo respeta) |
| Frontend (landing) | `/frontend/landing` | `Dockerfile`| `PORT` (idem) |
| Postgres  | (plugin de Railway, no un servicio del repo) | - | - |

Railway detecta el `Dockerfile` de cada carpeta automáticamente (también hay un `railway.json` en cada una fijando el healthcheck). No hace falta tocar nada de esos archivos salvo que cambie el puerto o la ruta de salud.

## 1. Crear el proyecto

1. Crear cuenta en [railway.com](https://railway.com) (tiene un trial con crédito gratis, después es pago por uso - para este tamaño de proyecto sale unos pocos dólares al mes).
2. "New Project" → "Deploy from GitHub repo" → elegir este repo.
3. Agregar el plugin de Postgres ("New" → "Database" → "PostgreSQL") dentro del mismo proyecto. Railway genera automáticamente las variables `DATABASE_URL`, `PGUSER`, `PGPASSWORD`, etc. — hay que mapearlas a las que usa este backend (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
4. Agregar el servicio del backend apuntando a este repo con Root Directory `/backend`.
5. Agregar el servicio del panel apuntando a este mismo repo con Root Directory `/frontend/panel`.
6. Agregar el servicio de la landing apuntando a este mismo repo con Root Directory `/frontend/landing`.

## 2. Variables de entorno

Las mismas que en `DEPLOY.md` (sección de variables obligatorias/recomendadas/opcionales del backend y de los frontends) - lo único que cambia es de dónde sale `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (del plugin de Postgres de Railway en vez de Supabase) y las URLs públicas (`*.up.railway.app` en vez de `*.onrender.com`/`*.vercel.app`).

## 3. Orden recomendado

1. Desplegar el backend primero, esperar que el healthcheck (`/actuator/health`) pase en verde y copiar su URL pública.
2. Cargar `NEXT_PUBLIC_API_URL` en el panel con esa URL y recién ahí desplegarlo.
3. Copiar la URL pública del panel y cargarla como `PANEL_ALLOWED_ORIGINS` en el backend, y como `NEXT_PUBLIC_PANEL_URL` en la landing.
4. Cargar el resto de las variables de la landing y desplegarla.
5. Actualizar el webhook de Twilio y el del Meta App Dashboard con la URL pública del backend.

## 4. Ventaja sobre el stack gratuito

Railway no duerme los servicios por inactividad (a diferencia del plan free de Render) y no tiene el límite de 60 conexiones directas de Supabase free - conviene evaluarlo de nuevo cuando el tráfico real lo justifique.
