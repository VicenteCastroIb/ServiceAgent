# Build multi-stage con output "standalone" (ver next.config.ts): la imagen
# final solo lleva el server mínimo, no todo node_modules.
#
# Importante: NEXT_PUBLIC_API_URL se "hornea" adentro del bundle en el build
# (Next.js inlinea las env NEXT_PUBLIC_* en tiempo de build, no las lee en
# runtime) - por eso viene como ARG y hay que cargarla como variable del
# servicio en Railway/Render para que quede disponible durante el build, no
# solo en runtime. Ver DEPLOY.md.

FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
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
