# ── Stage 1: dependencies ─────────────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && cp -R node_modules /prod_modules
RUN npm ci

# ── Stage 2: build ────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── Stage 3: development ──────────────────────────────────────────────────
FROM node:20-alpine AS development
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
EXPOSE 3000
CMD ["npm", "run", "start:dev"]

# ── Stage 4: production ───────────────────────────────────────────────────
FROM node:20-alpine AS production
LABEL org.opencontainers.image.title="CampusConnect API"
LABEL org.opencontainers.image.description="CampusConnect NestJS Backend"

WORKDIR /app

# Non-root user for security
RUN addgroup -g 1001 -S nodejs && adduser -S nestjs -u 1001

COPY --from=build  /app/dist          ./dist
COPY --from=deps   /prod_modules      ./node_modules
COPY package*.json ./

# Create log directory
RUN mkdir -p logs uploads && chown -R nestjs:nodejs logs uploads

USER nestjs

EXPOSE 3000
ENV NODE_ENV=production

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- "http://localhost:${PORT:-3000}/api/v1/health/ping" || exit 1

CMD ["node", "dist/src/main.js"]
