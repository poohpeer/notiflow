# Notiflow

Async notification platform demo built with Spring Boot, Kafka, PostgreSQL and Redis.

Notiflow accepts notification requests over HTTP, stores them durably, publishes an outbox event to Kafka and processes delivery asynchronously in a separate worker application.

## Architecture

```text
Browser (notiflow-frontend, React SPA)
  |
  v
notiflow-api
  |
  +--> PostgreSQL: notifications + outbox_events
  +--> Redis: rate limit
  |
  v  (transactional outbox)
notiflow-relay  --> Kafka topic: notiflow.notifications
                          |
                          v
                     notiflow-worker
                          |
                          +--> Mock providers: EMAIL, TELEGRAM, SMS, PUSH
                          |
                          v
                     Kafka topic: notiflow.notifications.dlq
```

The MVP is a small set of runnable applications, not a full microservice fleet:

- `notiflow-api`: REST API, validation, idempotency, rate limiting and writing the transactional outbox.
- `notiflow-relay`: polls the outbox table and publishes events to Kafka.
- `notiflow-worker`: Kafka consumer, retry policy, mock provider dispatch and DLQ publishing.
- `notiflow-frontend`: React SPA (dashboard, list/detail, create form) served by nginx.
- `notiflow-contracts`: shared request/response/event contracts.

## Technical Highlights

- Async processing with Kafka.
- Transactional outbox table between PostgreSQL and Kafka.
- Required `Idempotency-Key` header for safe client retries.
- Redis-backed per-channel/per-recipient rate limiting.
- Retry policy with `FAILED_RETRYABLE` and `DEAD_LETTERED` states.
- Mock providers for email, telegram, sms and push.
- OpenAPI Swagger UI.
- Spring Actuator health and Prometheus metrics.
- Grafana dashboard provisioned from repository files.
- React dashboard UI (current backlog, delivery-flow throughput, service health) reading the API and the Prometheus HTTP API.
- Docker Compose local environment with profiles for backend and frontend.

## Requirements

- Docker and Docker Compose.
- JDK 25 if running the backend from Maven/IDE locally.
- Node.js 22+ if running the frontend dev server (`npm run dev`); not needed for the Docker image.

The project is intentionally configured for Java 25 because that was selected for the MVP. The current source will not compile on Java 21 without changing `java.version`.

## Run

The stack is split into Docker Compose profiles so you can run the backend in Docker or in your IDE, and start the frontend on its own. Infrastructure (postgres, redis, kafka, prometheus, grafana) always starts; the backend apps and the UI are behind profiles.

Prometheus scrapes the apps at `host.docker.internal:8080/8081/8082`, and the frontend's nginx proxies `/api` and `/prom` to the host too. Both therefore work whether the backend runs in the IDE (port bound on the host) or in Docker (container port published to the host).

### Backend in the IDE (development)

Start only infrastructure, then run `notiflow-api` / `notiflow-worker` / `notiflow-relay` from IntelliJ:

```bash
docker compose up -d
```

### Backend in Docker

```bash
docker compose --profile backend up -d --build
```

### Frontend (independent of the backend)

```bash
docker compose --profile frontend up -d --build notiflow-frontend
```

UI at `http://localhost:5173`. The backend it talks to can be running in the IDE or in Docker.

For frontend development with hot reload (Vite proxies to `localhost:8080` / `localhost:9090`):

```bash
cd frontend
npm install
npm run dev
```

### Everything in Docker

```bash
docker compose --profile backend --profile frontend up -d --build
```

Services:

- Frontend UI: `http://localhost:5173`
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Worker actuator: `http://localhost:8081/actuator/health`
- Relay actuator: `http://localhost:8082/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

Grafana is preconfigured with Prometheus as the default datasource. Open Dashboards -> Notiflow -> Notiflow Overview.

## Create A Notification

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{
    "channel": "EMAIL",
    "recipient": "user@example.com",
    "subject": "Welcome",
    "message": "Hello from Notiflow",
    "metadata": {}
  }'
```

Response:

```json
{
  "notificationId": "00000000-0000-0000-0000-000000000000",
  "status": "ACCEPTED",
  "statusUrl": "/api/v1/notifications/00000000-0000-0000-0000-000000000000"
}
```

Check status:

```bash
curl http://localhost:8080/api/v1/notifications/{notificationId}
```

## Idempotency

`Idempotency-Key` is required on `POST /api/v1/notifications`.

- Same key + same payload returns the existing notification.
- Same key + different payload returns `409 Conflict`.
- Missing key returns `400 Bad Request`.

This prevents duplicate notifications when a client retries after a timeout or network failure.

## Retry And DLQ Demo

Trigger one retry, then success:

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: retry-once-001' \
  -d '{
    "channel": "SMS",
    "recipient": "+123456789",
    "message": "Your code is 1234",
    "metadata": {
      "mockFailure": "retryable-once"
    }
  }'
```

Trigger DLQ after configured max attempts:

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: dlq-001' \
  -d '{
    "channel": "PUSH",
    "recipient": "device-token",
    "message": "Push body",
    "metadata": {
      "mockFailure": "retryable"
    }
  }'
```

Trigger permanent failure without retry:

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: permanent-001' \
  -d '{
    "channel": "TELEGRAM",
    "recipient": "@demo",
    "message": "Telegram body",
    "metadata": {
      "mockFailure": "permanent"
    }
  }'
```

## Roadmap

- Real email provider through SMTP or MailHog.
- Telegram provider with bot token.
- Auth/JWT.
- Kubernetes manifests.
- CI pipeline.
