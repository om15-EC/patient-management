# 🏥 Patient Management System

A multi-service patient API built with Spring Boot. The gateway checks JWTs, patient changes go out through Kafka, and each service has its own Postgres database.

---

## 📌 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Communication Patterns](#communication-patterns)
- [Security](#security)
- [Features](#features)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Infrastructure](#infrastructure)

---

## Architecture Overview

```
Client (Postman / Browser)
           │
           ▼
    ┌─────────────────────────────────┐
    │         API Gateway :4004        │
    │  JWT · RBAC · Rate Limiting     │
    │  Consul lb:// Load Balancing    │
    └─────────────────────────────────┘
           │
    ┌──────┴──────────────────────┐
    ▼              ▼              ▼
Auth Service  Patient Service  Analytics Service
  :4005          :4000            :4002
  auth-db        patient-db       analytics-db
                    │
                    │ Kafka topic: patient
                    ▼
             Billing Service
                :4001
               billing-db
                    │
                    │ Kafka topic: billing.events
                    ▼
             Patient Service (status ACTIVE or FAILED)

Infrastructure:
  Kafka    :9092   — patient and billing events
  Redis    :6379   — cache · blacklist · rate limit · idempotency
  Consul   :8500   — service discovery
  ELK      :9200/:5601 — centralized logging
  Zipkin   :9411   — distributed tracing
```

---

## Tech Stack

| Category | Technology |
|---|---|
| **Framework** | Spring Boot 3.4.x · Spring Cloud 2024.0.1 |
| **Language** | Java 17 |
| **Database** | PostgreSQL (DB per service) |
| **Migrations** | Flyway |
| **Messaging** | Apache Kafka (KRaft mode) |
| **Serialization** | Protocol Buffers (protobuf) |
| **Caching** | Redis |
| **Service Discovery** | HashiCorp Consul |
| **API Gateway** | Spring Cloud Gateway (WebFlux) |
| **RPC** | gRPC |
| **Security** | JWT (JJWT) · Spring Security |
| **Resilience** | Resilience4j (Circuit Breaker) |
| **Logging** | ELK Stack (Elasticsearch · Logstash · Kibana) |
| **Tracing** | OpenTelemetry · Zipkin |
| **Email** | JavaMailSender · Mailtrap |
| **Containerization** | Docker · Docker Compose |

---

## Services

###  Auth Service (:4005)
Handles authentication and authorization.
- JWT access tokens (15 min) + refresh tokens (7 days)
- BCrypt password hashing (strength 12)
- Public registration always creates `ROLE_USER`
- Redis token blacklist on logout
- Admin force-logout also revokes the refresh token. Disable is permanent in the database.

###  Patient Service (:4000)
Core CRUD operations for patient management.
- REST API with pagination and filtering
- New patients stay `PENDING` until billing answers. No answer within 2 minutes, or a billing failure, sets `FAILED`.
- Outbox row is marked published only after Kafka acks the record
- Optional `Idempotency-Key` header. The response is cached in Redis for 24 hours.
- Redis caching (1 min list · 10 min single)
- Requires the gateway internal token plus `X-User-Email` / `X-User-Role`. A direct call with a forged role is rejected.

###  Billing Service (:4001)
Manages patient billing accounts.
- Kafka consumer creates the billing account, then publishes `billing.events`
- gRPC server is still in this service. Patient creation does not call it.
- Failed records are retried, then published to a DLT. The DLT listener logs them.

###  Analytics Service (:4002)
Event processing and enrichment.
- Kafka consumer for patient and billing events
- Wikipedia API — disease information enrichment
- RestCountries API — country data enrichment
- CompletableFuture — parallel API calls
- Circuit Breaker — prevents cascade failures
- Welcome email notifications via Mailtrap

###  API Gateway (:4004)
Single entry point for all clients.
- JWT validation + Redis blacklist check
- `ROLE_ADMIN` can call any method. `ROLE_USER` is GET only.
- Rate limiting (token bucket · Redis)
- Consul service discovery (`lb://` URIs)
- Replaces any client-sent `X-User-Email` / `X-User-Role` and sets them from the token, along with `X-Internal-Token`

---

## Communication Patterns

```
REST    clients → gateway → services
Kafka   patient, billing, and analytics
```

Kafka Topics:
  patient        → patient.created events (protobuf)
  billing.events → billing.created/failed (protobuf)
  patient.DLT    → failed patient events (DLQ)
  billing.events.DLT → failed billing events (DLQ)
```

---

## Security

The stack is HTTP on localhost, not TLS. Checks that are actually in the code:

```
1. JWT signature      — HMAC-SHA384, checked at the gateway
2. Token blacklist    — Redis, logout
3. User blacklist     — Redis, admin force-logout (refresh token included)
4. Role check         — gateway, and again inside patient-service
5. Internal token     — patient-service only trusts calls from the gateway
6. Rate limit         — Redis token bucket at the gateway
7. @PreAuthorize      — analytics summary and the admin API
```

| Role | Access |
|---|---|
| `ROLE_ADMIN` | All operations (GET · POST · PUT · PATCH · DELETE) |
| `ROLE_USER` | GET only |
| Analytics | `ROLE_ADMIN` only |

---

## Features

### Distributed Systems Patterns
- ✅ **Saga** — patient stays `PENDING` until billing reports back, or the 2 minute timeout fires
- ✅ **Outbox** — the patient row and the event row commit together. Kafka publish happens after the commit, and only an acked send is marked published.
- ✅ **Circuit breaker** — Resilience4j around the Wikipedia and country calls in analytics
- ✅ **Idempotency-Key** — optional. Same key returns the cached create response for 24 hours. The request body is not hashed.
- ✅ **DLT** — a record that still fails after 3 retries is published to `*.DLT` and logged

### Observability
- ✅ **Centralized Logging** — ELK Stack (Kibana index: `patient-management-*`)
- ✅ **Distributed Tracing** — OpenTelemetry + Zipkin (100% sampling)
- ✅ **Health checks** — Actuator, used by Consul and Compose

### Performance
- ✅ **Redis cache** — patient list for 1 minute, one patient for 10 minutes
- ✅ **Parallel calls** — analytics fetches disease and country info together
- ✅ **Kafka** — patient creation does not wait on billing. The HTTP call returns while the patient is still `PENDING`.

---

## Getting Started

### Prerequisites

```bash
Docker Desktop (with Docker Compose)
Java 17+
Maven 3.9+
```

### 1. Clone repository

```bash
git clone https://github.com/om15-EC/patient-management.git
cd patient-management
```

### 2. Create `.env` file

```bash
cp .env.example .env
# Edit .env with your values
```

Required environment variables:
```env
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
JWT_SECRET=your_base64_encoded_secret_min_32_chars
JWT_EXPIRATION=900000
MAILTRAP_HOST=sandbox.smtp.mailtrap.io
MAILTRAP_PORT=2525
MAILTRAP_USERNAME=your_mailtrap_username
MAILTRAP_PASSWORD=your_mailtrap_password
```

Leave `REDIS_PASSWORD` and `INTERNAL_API_SECRET` blank unless you want your own values. Compose then uses `dev-redis-password` and `dev-internal-token`. Kafka and the billing host are already set in `docker-compose.yml`.

### 3. Build proto-common

```bash
cd proto-common
mvn clean install -DskipTests
cd ..
```

### 4. Start all services

```bash
docker compose up --build -d
```

### 5. Verify all services are running

```bash
docker compose ps
```

Most containers should be `healthy`. Auth can stay `unhealthy`: its healthcheck calls `curl`, and that image does not have `curl`. Check `docker compose logs auth-service` if login fails.

### 6. Register a user

Public registration only accepts `ROLE_USER`. There is no `name` field.

```bash
curl -X POST http://localhost:4004/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin@1234",
    "role": "ROLE_USER"
  }'
```

To make that user an admin, update the auth database:

```bash
docker exec -it auth-service-db psql -U your_db_username -d db \
  -c "UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';"
```

`your_db_username` is the `DB_USERNAME` value from `.env`.

### 7. Login and get token

```bash
curl -X POST http://localhost:4004/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin@1234"
  }'
```

---

## API Documentation

### Auth Service (`/api/v1/auth`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/register` | None | Register a `ROLE_USER` |
| POST | `/login` | None | Login → get JWT tokens |
| POST | `/logout` | Bearer | Blacklist current token |
| POST | `/refresh` | Refresh-Token header | Get new access token |

### Admin (`/api/admin`) — ROLE_ADMIN only

| Method | Endpoint | Description |
|---|---|---|
| POST | `/logout/{email}` | Force logout. Kills the refresh token too. |
| POST | `/disable/{email}` | Disable the account in Postgres and revoke sessions |

### Patient Service (`/api/patients`) — via Gateway

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/` | USER/ADMIN | Get all patients (paginated) |
| GET | `/{id}` | USER/ADMIN | Get patient by ID |
| POST | `/` | ADMIN | Create patient |
| PUT | `/{id}` | ADMIN | Update patient |
| PATCH | `/{id}` | ADMIN | Partial update |
| DELETE | `/{id}` | ADMIN | Delete patient |

---

## Infrastructure

### Service Ports

| Service | Port | Debug Port |
|---|---|---|
| API Gateway | 4004 | 5009 |
| Auth Service | 4005 | 5005 |
| Patient Service | 4000 | 5006 |
| Billing Service | 4001 | 5007 |
| Analytics Service | 4002 | 5008 |
| Kafka | 9092 | — |
| Redis | 6379 | — |
| Consul UI | 8500 | — |
| Elasticsearch | 9200 | — |
| Kibana | 5601 | — |
| Zipkin | 9411 | — |


## Project Structure

```
patient-management/
├── proto-common/               ← Shared protobuf contracts
│   └── src/main/proto/
│       ├── billing_service.proto
│       ├── patient_event.proto
│       └── billing_event.proto
├── auth-service/               ← Authentication service
├── patient-service/            ← Patient CRUD service
├── billing-service/            ← Billing service (Kafka consumer, gRPC server unused by patient create)
├── analytics-service/          ← Analytics + enrichment service
├── api-gateway/                ← Spring Cloud Gateway
├── logstash/
│   └── pipeline/
│       └── logstash.conf       ← Log pipeline config
├── docker-compose.yml          ← Full stack orchestration
├── .env.example                ← Environment template
├── Dockerfile.patient-service
├── Dockerfile.billing-service
└── Dockerfile.analytics-service
```

---

## License

MIT License — feel free to use for learning and reference.
