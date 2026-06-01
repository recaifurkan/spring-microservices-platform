# Spring Microservices Platform

`spring-microservices-platform` is a multi-module Spring Boot platform for authentication, routing, observability, and business services.

It combines OAuth2 / JWT authentication, centralized configuration, service discovery, gateway routing, Feign-based service-to-service calls, OpenTelemetry tracing, structured logging, and a handful of domain services such as users, products, orders, payments, notifications, cart, and ACS.

---

## What you can learn from this project

This repository is intended as a practical learning playground for:

- Spring Authorization Server and token issuance
- OAuth2 client and resource server flows
- Centralized configuration with Spring Cloud Config
- Service registration and lookup with Eureka
- API gateway routing and JWT enforcement
- Feign clients, fallbacks, and token propagation
- Scope-based authorization instead of ad-hoc role checks
- Distributed tracing with OpenTelemetry
- Log shipping and visualization with Loki and Grafana
- Local development with either embedded H2 databases or PostgreSQL via Docker Compose

---

## Modules

| Module | Port | Purpose |
|---|---:|---|
| `config-server` | `8888` | Central Spring Cloud Config Server |
| `discovery-server` | `8761` | Eureka service registry |
| `auth-server` | `9000` | Spring Authorization Server and user management API |
| `api-gateway` | `8090` | Reactive gateway and authentication entry point |
| `frontend-service` | `8070` | Browser-facing UI / aggregator service |
| `user-service` | `8082` | User profile and user administration APIs |
| `product-service` | `8084` | Product catalog and search APIs |
| `order-service` | `8085` | Order creation, tracking, and payment orchestration |
| `payment-service` | `8086` | Payment and refund workflows |
| `notification-service` | `8087` | Notification delivery APIs |
| `cart-service` | `8088` | Shopping cart APIs |
| `acs-service` | `8089` | 3D Secure / ACS demo service |

Shared helpers live in `common`, while infrastructure assets and local observability config live under `docker/`.

---

## Architecture overview

```text
Browser / API consumer
        |
        v
api-gateway (8090)  ---> routes and protects downstream calls
        |
        +--> frontend-service (8070)
        +--> user-service (8082)
        +--> product-service (8084)
        +--> order-service (8085)
        +--> payment-service (8086)
        +--> notification-service (8087)
        +--> cart-service (8088)
        +--> acs-service (8089)

auth-server (9000) issues JWTs and publishes JWKs
config-server (8888) provides shared and service-specific configuration
discovery-server (8761) lets services find each other
```

Typical request flow:

1. A client authenticates against `auth-server`.
2. `auth-server` issues a signed JWT.
3. `api-gateway` validates the token and forwards the request.
4. Downstream services authorize requests using scopes in the token.
5. Logs and traces are collected through the observability stack.

---

## Repository layout

- `auth-server` — authorization server, user registration, user administration
- `api-gateway` — reactive gateway, JWT validation, routing
- `frontend-service` — browser UI, OAuth2 login, cart and checkout orchestration
- `user-service` — user profile management
- `product-service` — catalog, filtering, search, stock awareness
- `order-service` — order creation, status tracking, payment callbacks
- `payment-service` — payment processing and 3DS orchestration
- `notification-service` — notification generation and read tracking
- `cart-service` — persistent cart storage and manipulation
- `acs-service` — 3D Secure challenge simulation
- `config-server` — shared and service-specific configuration files
- `discovery-server` — Eureka registration
- `common` — shared Feign, OpenAPI, JWT, and security helpers

---

## Prerequisites

- Java 17
- Maven 3.9+ recommended
- Docker Desktop if you want PostgreSQL and the observability stack
- `curl` for manual API checks
- `python3` for the token extraction examples below

---

## Quick start with `manage.sh`

The recommended entry point is the root-level `manage.sh` script.

### Make the script executable

```bash
chmod +x ./manage.sh
```

### Start the supporting infrastructure

Use this when you want the Docker-based local environment:

```bash
./manage.sh env up
```

This starts PostgreSQL, pgAdmin, Jaeger, Prometheus, Loki, and Grafana.

### Start all application services with PostgreSQL

```bash
./manage.sh start --env local --no-build
```

### Start all application services with embedded H2

```bash
./manage.sh start --env default --no-build
```

If you want the script to rebuild before launching, omit `--no-build`.

### Start a single service

```bash
./manage.sh start auth-server --env local --no-build
./manage.sh start api-gateway --env local --no-build
```

### Check status, logs, or stop everything

```bash
./manage.sh status
./manage.sh logs auth-server
./manage.sh stop
```

### Stop Docker infrastructure

```bash
./manage.sh env down
./manage.sh env down --volumes
```

`env down --volumes` also removes persisted database volumes.

---

## How to test the services

### 1) Discovery Server

Open the Eureka dashboard:

```text
http://localhost:8761
```

You should see all active services registered there.

### 2) Config Server

Verify that configuration is being served correctly:

```bash
curl http://localhost:8888/auth-server/default
curl http://localhost:8888/api-gateway/default
curl http://localhost:8888/user-service/default
```

### 3) Auth Server

Check the JWK endpoint:

```bash
curl http://localhost:9000/oauth2/jwks
```

Request a token using client credentials:

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u client-app:secret \
  -d "grant_type=client_credentials&scope=read write"
```

### 4) Gateway + downstream services

Once you have a token, call secured services through the gateway:

```bash
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u client-app:secret \
  -d "grant_type=client_credentials&scope=read write" \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

curl http://localhost:8090/actuator/health
curl http://localhost:8090/api/users/profile -H "Authorization: Bearer $TOKEN"
curl http://localhost:8090/api/products -H "Authorization: Bearer $TOKEN"
```

### 5) Direct service URLs

Use these when you want to bypass the gateway during debugging:

- `auth-server`: `http://localhost:9000`
- `api-gateway`: `http://localhost:8090`
- `frontend-service`: `http://localhost:8070`
- `user-service`: `http://localhost:8082`
- `product-service`: `http://localhost:8084`
- `order-service`: `http://localhost:8085`
- `payment-service`: `http://localhost:8086`
- `notification-service`: `http://localhost:8087`
- `cart-service`: `http://localhost:8088`
- `acs-service`: `http://localhost:8089`

### 6) Health checks

Most services expose Spring Boot actuator health endpoints:

```bash
curl http://localhost:8090/actuator/health
curl http://localhost:9000/actuator/health
curl http://localhost:8082/actuator/health
```

---

## Configuration model

Shared configuration lives under `config-server/src/main/resources/config/`.

- `application.yml` contains shared defaults used by all services.
- Each service has its own `application.yml` for service-specific settings.
- `*-local.yml` files switch services to PostgreSQL-backed local profiles.

The config server can also be switched to a Git-backed repository if you want to externalize configuration further.

---

## Security model

The auth server issues signed JWT access tokens and exposes the public JWK set so that other services can verify tokens without sharing private keys.

Key concepts used in the project:

- OAuth2 client credentials flow for machine-to-machine access
- authorization code flow for interactive login scenarios
- scope-based authorization on protected APIs
- custom JWT claims for user metadata and granted permissions
- resource server validation through issuer and JWK configuration

Typical claims include:

```json
{
  "sub": "client-app",
  "iss": "http://localhost:9000",
  "scope": "read write",
  "client_id": "client-app"
}
```

The exact claim set depends on the grant type and the token customizer used by the auth server.

---

## Observability stack

The project includes a local observability setup for learning purposes:

- OpenTelemetry collector for traces
- Jaeger for distributed tracing UI
- Loki for logs
- Prometheus for metrics and span metrics
- Grafana for dashboards and exploration

This setup makes it easier to understand request flow, token propagation, and service-to-service behavior.

---

## Testing notes

- Use `./manage.sh status` to quickly see which services are running.
- Use the `logs/` folders to inspect runtime output.
- The `order-service` E2E tests cover a full shopping and payment flow.
- The gateway and auth-server endpoints are good starting points for smoke testing.

---

## Technology stack

- Java 17
- Spring Boot 3.3.x
- Spring Cloud 2023.x
- Spring Authorization Server
- Spring Security OAuth2 Resource Server
- Spring Cloud Netflix Eureka
- Spring Cloud Config
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- OpenTelemetry
- Logback + Loki
- H2 and PostgreSQL


