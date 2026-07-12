# notiflow-frontend

React SPA for Notiflow — dashboard, notifications list/detail, and a create form on top of `notiflow-api` and Prometheus.

Stack: Vite + React 18 + TypeScript + React Query + React Router + Tailwind + Recharts. Tests: Vitest + Testing Library.

## Quick start

```bash
npm install
npm run dev          # http://localhost:5173
```

The dev server proxies requests so the SPA stays same-origin (no CORS config on the backend):

| Path    | Target                  |
| ------- | ----------------------- |
| `/api`  | `http://localhost:8080` (notiflow-api) |
| `/prom` | `http://localhost:9090` (Prometheus, prefix stripped) |

So the backend infra must be running — from the repo root: `docker compose up -d` (infra) plus the api/worker/relay, or `docker compose --profile backend up -d --build`.

## Scripts

| Command             | What it does                          |
| ------------------- | ------------------------------------- |
| `npm run dev`       | Vite dev server on :5173              |
| `npm run build`     | Typecheck (`tsc --noEmit`) + prod build to `dist/` |
| `npm run preview`   | Serve the production build            |
| `npm run typecheck` | Types only                            |
| `npm test`          | Vitest once                           |
| `npm run test:watch`| Vitest in watch mode                  |
| `npm run coverage`  | Vitest with v8 coverage               |

## Layout

```
src/
  api/         REST client, notification + Prometheus metric queries, shared types
  components/  Layout, Logo, badges, KPI cards, chart, toast, pagination
  hooks/       useNotifications, useMetrics (React Query wrappers)
  lib/         formatting helpers
  pages/       Dashboard, Notifications, NotificationDetail, CreateNotification
  router.tsx   routes
public/        brand assets — notiflow-logo.svg, favicon.ico + PNG sizes
```

Creating a notification requires an `Idempotency-Key`; the form generates one per submit (see `src/api/notifications.ts`).

## Configuration

Both bases default to same-origin paths and only need setting for a cross-origin deployment (see `.env.example`):

- `VITE_API_BASE` — default `/api`
- `VITE_PROM_BASE` — default `/prom`

## Docker

```bash
docker compose --profile frontend up -d --build notiflow-frontend   # UI at :5173
```

Multi-stage build (`Dockerfile`): node builds `dist/`, nginx serves it and reverse-proxies `/api` and `/prom` via `host.docker.internal`, so the backend can run either in the IDE or in Docker. SPA routes fall back to `index.html` (`nginx.conf`).
