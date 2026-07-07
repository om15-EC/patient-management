# 🏥 Patient Management System

A **production-grade microservices system** built with Spring Boot, demonstrating real-world distributed systems architecture with enterprise security, observability, and AI integration (TO-DO).

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
                    │ gRPC
                    ▼
             Billing Service
                :4001
               billing-db

Infrastructure:
  Kafka    :9092   — async event streaming
  Redis    :6379   — cache · blacklist · rate limiting
  Consul   :8500   — service discovery
  ELK      :9200/:5601 — centralized logging
  Zipkin   :9411   — distributed tracing
```

---

## Tech Stack

| Category | Technology |
|---|---|
| **Framework** | Spring Boot 3.4.x · Spring Cloud 2024.0.1 |
| **Language** | Java 21 |
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
- Redis token blacklist for instant revocation
- Admin force-logout and permanent user disable

###  Patient Service (:4000)
Core CRUD operations for patient management.
- REST API with pagination and filtering
- Saga pattern — patient status: `PENDING → ACTIVE/FAILED`
- Outbox pattern — guaranteed Kafka event delivery
- Idempotency keys — duplicate request prevention
- Redis caching (1 min list · 10 min single)

###  Billing Service (:4001)
Manages patient billing accounts.
- gRPC server — fast synchronous billing account creation
- Kafka consumer — async fallback via Outbox pattern
- Dead Letter Queue (DLQ) — failed message handling

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
- RBAC enforcement (ROLE_ADMIN · ROLE_USER)
- Rate limiting (token bucket · Redis)
- Consul service discovery (lb:// URIs)
- Request header injection (X-User-Email · X-User-Role)

---

## Communication Patterns

```
┌─────────────────────────────────────────────────┐
│  Pattern    │ Used For                           │
├─────────────┼────────────────────────────────────│
│  REST       │ External clients → Gateway         │
│  gRPC       │ Patient → Billing (sync fast path) │
│  Kafka      │ Async events between services      │
│                                                  │
└─────────────────────────────────────────────────┘

Kafka Topics:
  patient        → patient.created events (protobuf)
  billing.events → billing.created/failed (protobuf)
  patient.DLT    → failed patient events (DLQ)
  billing.events.DLT → failed billing events (DLQ)
```

---

## Security

7 layers of defense in depth:

```
1. HTTPS/TLS          — encrypted transport
2. JWT Validation     — HMAC-SHA384 signature verification
3. Token Blacklist    — Redis · instant revocation on logout
4. User Blacklist     — Redis · admin force-logout
5. RBAC               — role-based access at gateway
6. Rate Limiting      — token bucket · Redis · 429 response
7. @PreAuthorize      — method-level security
```

| Role | Access |
|---|---|
| `ROLE_ADMIN` | All operations (GET · POST · PUT · PATCH · DELETE) |
| `ROLE_USER` | GET only |
| Analytics | `ROLE_ADMIN` only |

---

## Features

### Distributed Systems Patterns
- ✅ **Saga Pattern** — choreography-based distributed transactions
- ✅ **Outbox Pattern** — guaranteed at-least-once event delivery
- ✅ **Circuit Breaker** — Resilience4j with fallback responses
- ✅ **Idempotency Keys** — duplicate request prevention with body hash validation
- ✅ **Dead Letter Queue** — failed Kafka messages never dropped

### Observability
- ✅ **Centralized Logging** — ELK Stack (Kibana index: `patient-management-*`)
- ✅ **Distributed Tracing** — OpenTelemetry + Zipkin (100% sampling)
- ✅ **Health Checks** — Spring Actuator + Consul health checks
- ✅ **Graceful Shutdown** — 30s drain period for in-flight requests

### Performance
- ✅ **Redis Caching** — multi-layer with TTL per cache type
- ✅ **Parallel API calls** — CompletableFuture for external enrichment
- ✅ **gRPC fast path** — binary protocol for internal sync calls
- ✅ **Kafka async** — non-blocking event processing

---

## Getting Started

### Prerequisites

```bash
Docker Desktop (with Docker Compose)
Java 21+
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
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
BILLING_SERVICE_ADDRESS=billing-service
BILLING_SERVICE_GRPC_PORT=9001
MAILTRAP_HOST=sandbox.smtp.mailtrap.io
MAILTRAP_PORT=2525
MAILTRAP_USERNAME=your_mailtrap_username
MAILTRAP_PASSWORD=your_mailtrap_password
```

### 3. Build proto-common

```bash
cd proto-common
mvn clean install -DskipTests
cd ..
```

### 4. Start all services

```bash
docker compose up -d
```

### 5. Verify all services are running

```bash
docker compose ps
```

Expected: all containers `healthy`

### 6. Register first admin user

```bash
curl -X POST http://localhost:4004/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "Admin@1234",
    "role": "ROLE_ADMIN"
  }'
```

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
| POST | `/register` | None | Register new user |
| POST | `/login` | None | Login → get JWT tokens |
| POST | `/logout` | Bearer | Blacklist current token |
| POST | `/refresh` | Refresh-Token header | Get new access token |

### Admin (`/api/admin`) — ROLE_ADMIN only

| Method | Endpoint | Description |
|---|---|---|
| POST | `/logout/{email}` | Force logout a user |
| POST | `/disable/{email}` | Permanently disable user |
| POST | `/enable/{email}` | Re-enable disabled user |

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
├── billing-service/            ← Billing service (gRPC server)
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
