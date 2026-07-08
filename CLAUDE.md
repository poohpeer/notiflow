# Notiflow — project guide for AI assistants

Async notification platform demo: Spring Boot + Kafka + PostgreSQL + Redis backend, React frontend. Accepts notification requests over HTTP, persists them durably, and delivers them asynchronously through a transactional-outbox → Kafka → worker pipeline.

## Architecture

```
Browser (frontend, React SPA)
  → notiflow-api      writes notification + outbox_events (one DB txn)
  → notiflow-relay    polls outbox (SKIP LOCKED) → Kafka topic notiflow.notifications
  → notiflow-worker   consumes Kafka, dispatches to providers, retries, DLQ
```

Delivery is intentionally split so each stage scales independently. The api never writes to Kafka directly — it writes the outbox row in the same transaction as the notification; relay publishes it. This guarantees the message is sent iff the notification is committed.

## Modules (Maven multi-module, parent `pom.xml`)

- `notiflow-contracts` — shared records/enums (`NotificationRequest`, `NotificationCreatedEvent`, `ProviderResult`, `NotificationStatus`, `NotificationChannel`, `FailureType`). No Spring. Depended on by all services.
- `notiflow-api` — REST API (`POST/GET /api/v1/notifications`), validation, idempotency, Redis rate limiting, writes the outbox. Port 8080.
- `notiflow-relay` — scheduled outbox publisher, `SKIP LOCKED` batch, N-replica safe, transitions `ACCEPTED → QUEUED`. Port 8082.
- `notiflow-worker` — Kafka consumer, retry policy, mock providers, DLQ publishing. Port 8081.
- `frontend` — Vite + React + TypeScript + Tailwind + React Query SPA. Dashboard, notifications list/detail, create form. Dev port 5173.

Java package root: `com.alex.notiflow.<module>`.

## Key patterns (know these before changing them)

- **Transactional outbox** — `NotificationService.create` saves `NotificationEntity` + `OutboxEventEntity` in one `@Transactional`. Do not publish to Kafka from the api.
- **Idempotency** — `Idempotency-Key` header required on create. Same key + same payload hash (`RequestHasher`, order-independent metadata) → returns the existing notification; same key + different hash → 409 `IdempotencyConflictException`; missing → `MissingIdempotencyKeyException` (400).
- **Rate limiting** — `RateLimiter` uses Redis `setIfAbsent` + `increment` per `channel:recipient` window. (A prior race condition was fixed here — keep it atomic.)
- **Retry / DLQ** — `NotificationProcessor` loops up to `notiflow.retry.max-attempts`. `PERMANENT` → `FAILED_PERMANENT` immediately; `RETRYABLE` → retries; exhausted → `DEAD_LETTERED` + publish to `notiflow.notifications.dlq`. Terminal statuses (`SENT`, `FAILED_PERMANENT`, `DEAD_LETTERED`) are skipped (idempotent consumption).
- **Mock delivery** — `MockNotificationProvider` reads `metadata.mockFailure` (`permanent`, `retryable`, `retryable-once`) to drive demo failures.

## Build & test

- JDK 25 (`java.version=25`), Spring Boot 4.0.6. Won't compile on 21.
- Build: `./mvnw -q verify` (or `mvn`). Tests: JUnit 5 + AssertJ + Mockito, plain unit tests (no Spring context) — mock collaborators, use `SimpleMeterRegistry` for `MeterRegistry`. Testcontainers BOM is available for integration tests if needed.
- Frontend: `cd frontend && npm install && npm run dev` (or `npm run build`).

## Run (Docker Compose profiles)

Infra (postgres, redis, kafka, prometheus, grafana) always starts; apps and UI are behind profiles.

- Backend in IDE (dev): `docker compose up -d` (infra only), then run api/worker/relay from the IDE.
- Backend in Docker: `docker compose --profile backend up -d --build`.
- Frontend alone: `docker compose --profile frontend up -d --build notiflow-frontend` (UI at :5173).
- Everything: `docker compose --profile backend --profile frontend up -d --build`.

Prometheus and the frontend nginx both reach the backend via `host.docker.internal:PORT`, so it works whether the backend runs in the IDE or in Docker. Config defaults point to `localhost` (IDE); Compose overrides them to service names (`postgres`, `kafka`, `redis`).

Ports: api 8080, worker 8081, relay 8082, prometheus 9090, grafana 3000, frontend 5173.

## Conventions

- Lombok everywhere (`@RequiredArgsConstructor`, `@Getter/@Setter`, `@Slf4j`).
- Contracts are Java records. Entities are JPA `@Entity` with Lombok accessors.
- `application.yml` uses `${ENV:default}` — defaults are the local/IDE values.
- Config via `@ConfigurationProperties` records (`RateLimitProperties`, `RetryProperties`, `KafkaTopicsProperties`).
