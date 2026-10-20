# Uber Backend

A ride-hailing backend foundation built with Java 21 and Spring Boot.

## Current foundation

- Spring Boot REST API
- PostgreSQL and Flyway migrations
- Request validation and Actuator health/metrics endpoints
- Docker image and local Docker Compose environment
- Automated health endpoint and authentication service tests
- Stateless JWT authentication with BCrypt password hashing
- Role-aware accounts: `RIDER`, `DRIVER`, and `ADMIN` (admin self-registration is blocked)
- Configurable Tomcat and HikariCP connection-pool settings

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
# Generate a 256-bit key encoded as Base64 and save it in your shell environment.
export APP_TOKEN_KEY="$(openssl rand -base64 32)"
docker compose up --build
```

Register a rider (password must be at least 12 characters):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \\
  -H 'Content-Type: application/json' \\
  -d '{"email":"rider@example.com","password":"correct-horse-battery-staple"}'
```

Log in with `POST /api/v1/auth/login` using the same email and password. The response contains a short-lived bearer token. Send it as `Authorization: Bearer <accessToken>` when calling protected endpoints. Keep `APP_TOKEN_KEY` private and stable across app restarts.

Check the API:

```bash
curl http://localhost:8080/api/v1/health
```

Stop the services with `docker compose down`. Add `-v` only when you intentionally want to delete local database data.

## Performance target

The initial engineering target is **10,000 requests per minute (about 167 requests/second)**. This is a target, not a benchmark claim. Actual capacity depends on endpoint cost, database queries, payload size, hardware, and traffic distribution. Run load tests before claiming the target is met.

## Architecture direction

Start with a modular monolith, not premature microservices:

- Identity and access
- Rider and driver profiles
- Driver availability and location
- Ride lifecycle and dispatch
- Pricing and payments
- Notifications

Use PostgreSQL as the source of truth. Introduce Redis for ephemeral driver-location/availability reads and caching when measurements justify it. Use a durable queue for asynchronous notifications and other background work. Keep location updates and dispatch isolated behind explicit interfaces so they can scale independently later.

## Production checklist

- Replace development database credentials with secrets.
- Add token refresh/revocation, rate limiting, and idempotency for write endpoints.
- Add structured logs, distributed tracing, and metrics.
- Configure TLS, reverse proxy/load balancer, readiness/liveness probes, and graceful shutdown.
- Add integration tests and database migration tests.
- Load test with realistic traffic and monitor p95/p99 latency, error rate, CPU, memory, DB pool saturation, and query latency.

Do not increase thread or connection-pool values blindly. More concurrency can overload PostgreSQL instead of improving throughput.