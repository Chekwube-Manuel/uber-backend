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
- Separate rider and driver profile APIs with validation and unique phone/vehicle identifiers
- Admin-only driver verification and driver online/offline state
- Driver location updates and bounded nearby-driver search for approved, online drivers
- Configurable Tomcat and HikariCP connection-pool settings

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
export APP_TOKEN_KEY="$(openssl rand -base64 32)"
docker compose up --build
```

Register a rider (password must be at least 12 characters):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"rider@example.com","password":"correct-horse-battery-staple"}'
```

Log in with `POST /api/v1/auth/login` using the same email and password. The response contains a short-lived bearer token. Send it as `Authorization: Bearer <accessToken>` when calling protected endpoints. Keep `APP_TOKEN_KEY` private and stable across app restarts.

Create or update a rider profile with `PUT /api/v1/rider/profile` and body `{"fullName":"Test Rider","phoneNumber":"+2348012345678"}`.

Register a driver by passing `"role":"DRIVER"` to `/api/v1/auth/register`, then call `PUT /api/v1/driver/profile` with body `{"fullName":"Test Driver","phoneNumber":"+2348098765432","vehicleMake":"Toyota","vehicleModel":"Corolla","vehicleColor":"Silver","vehiclePlate":"ABC 123XY"}`.

### Driver verification and presence

Drivers start in `PENDING` verification status. A trusted operator must provision the first administrator through direct, controlled database access; admin self-registration is intentionally disabled. For an existing account, the operator can promote it with a reviewed, one-time SQL operation such as:

```sql
UPDATE app_users SET role = 'ADMIN' WHERE email = 'trusted-admin@example.com';
```

Protect database access, verify the account identity first, and do not expose this operation through a public endpoint.

An administrator can approve or reject a driver using `PUT /api/v1/admin/drivers/{driverId}/verification`:

```json
{"decision":"APPROVED"}
```

Only approved drivers can go online. Driver endpoint examples (all require that driver's bearer token):

```bash
curl -X PUT http://localhost:8080/api/v1/driver/availability \
  -H "Authorization: Bearer $DRIVER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"available":true}'

curl -X PUT http://localhost:8080/api/v1/driver/location \
  -H "Authorization: Bearer $DRIVER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"latitude":6.5244,"longitude":3.3792}'
```

Riders can query nearby drivers with their rider token: `GET /api/v1/rider/nearby-drivers?latitude=6.5244&longitude=3.3792&radiusKm=5`. The query returns at most 100 approved, online drivers with location updates from the last two minutes, ordered by distance. This is a discovery primitive, not yet ride dispatch or a reservation/locking mechanism.

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

PostgreSQL remains the source of truth. Nearby-driver lookup currently uses a bounding-box filter and Haversine distance calculation in PostgreSQL; benchmark it with realistic data. For high-frequency location updates or larger geographic datasets, evaluate PostGIS and/or Redis geospatial indexes based on measured latency and consistency needs. Keep location updates and dispatch behind explicit interfaces so they can scale independently later.

## Production checklist

- Replace development database credentials with secrets.
- Add token refresh/revocation, rate limiting, and idempotency for write endpoints.
- Add structured logs, distributed tracing, and metrics.
- Configure TLS, reverse proxy/load balancer, readiness/liveness probes, and graceful shutdown.
- Add integration tests and database migration tests.
- Load test with realistic traffic and monitor p95/p99 latency, error rate, CPU, memory, DB pool saturation, and query latency.

Do not increase thread or connection-pool values blindly. More concurrency can overload PostgreSQL instead of improving throughput.