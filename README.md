# Notiflow

Async notification platform demo built with Spring Boot, Kafka, PostgreSQL and Redis.

Notiflow accepts notification requests over HTTP, stores them durably, publishes an outbox event to Kafka and processes delivery asynchronously in a separate worker application.

## Architecture

```text
Client
  |
  v
notiflow-api
  |
  +--> PostgreSQL notifications + outbox_events
  +--> Redis rate limit
  |
  v
Kafka topic: notiflow.notifications
  |
  v
notiflow-worker
  |
  +--> Mock providers: EMAIL, TELEGRAM, SMS, PUSH
  |
  v
Kafka topic: notiflow.notifications.dlq
```

The MVP uses two runnable applications, not a full microservice fleet:

- `notiflow-api`: REST API, validation, idempotency, rate limiting and outbox publishing.
- `notiflow-worker`: Kafka consumer, retry policy, mock provider dispatch and DLQ publishing.
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
- Docker Compose local environment.

## Requirements

- Docker and Docker Compose.
- JDK 25 if running Maven locally.

The project is intentionally configured for Java 25 because that was selected for the MVP. The current source will not compile on Java 21 without changing `java.version`.

## Run

```bash
docker compose up --build
```

Services:

- API: `http://localhost:8080`
- Worker actuator: `http://localhost:8081/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
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
- Dashboard UI with realtime status updates.
- Kubernetes manifests.
- CI pipeline.
