# Build multi-stage con output "standalone" (ver next.config.ts) - mismo
# patrón que frontend/panel/Dockerfile: la imagen final solo lleva el server
# mínimo, no todo node_modules.
#
# Importante: las variables NEXT_PUBLIC_* se "hornean" adentro del bundle en
# el build (Next.js las inlinea en tiempo de build, no las lee en runtime) -
# por eso vienen como ARG y hay que cargarlas como variables del servicio en
# Railway/Render para que estén disponibles durante el build, no solo en
# runtime. Ver DEPLOY.md.

FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_WHATSAPP_SALES_NUMBER
ARG NEXT_PUBLIC_PANEL_URL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_PUBLIC_GA_ID
ARG NEXT_PUBLIC_META_PIXEL_ID
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
ENV NEXT_PUBLIC_WHATSAPP_SALES_NUMBER=${NEXT_PUBLIC_WHATSAPP_SALES_NUMBER}
ENV NEXT_PUBLIC_PANEL_URL=${NEXT_PUBLIC_PANEL_URL}
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}
ENV NEXT_PUBLIC_GA_ID=${NEXT_PUBLIC_GA_ID}
ENV NEXT_PUBLIC_META_PIXEL_ID=${NEXT_PUBLIC_META_PIXEL_ID}
RUN npm run build

FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

COPY --from=build /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
