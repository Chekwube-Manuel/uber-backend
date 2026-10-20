# Uber Backend

A ride-hailing backend foundation built with Java 21 and Spring Boot.

## Current foundation

- Spring Boot REST API
- PostgreSQL and Flyway migrations
- Request validation and Actuator health/metrics endpoints
- Docker image and local Docker Compose environment
- Stateless JWT authentication with BCrypt password hashing
- Role-aware accounts: `RIDER`, `DRIVER`, and `ADMIN` (admin self-registration is blocked)
- Rider and driver profiles, driver verification, availability, and location discovery
- Ride request, driver acceptance, lifecycle transitions, cancellation, and history endpoints
- Configurable fare-estimate rates, Tomcat threads, and HikariCP connection pool
- Distance-ranked dispatch queue and Server-Sent Events (SSE) ride-request stream

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
export APP_TOKEN_KEY="$(openssl rand -base64 32)"
docker compose up --build
```

Register and log in using `POST /api/v1/auth/register` and `POST /api/v1/auth/login`. Registration requires a password of at least 12 characters. Send the returned token as `Authorization: Bearer <accessToken>` to protected endpoints.

Create or update a rider profile with `PUT /api/v1/rider/profile` and body `{"fullName":"Test Rider","phoneNumber":"+2348012345678"}`.

Register a driver by passing `"role":"DRIVER"` to registration, then call `PUT /api/v1/driver/profile` with body `{"fullName":"Test Driver","phoneNumber":"+2348098765432","vehicleMake":"Toyota","vehicleModel":"Corolla","vehicleColor":"Silver","vehiclePlate":"ABC 123XY"}`.

## Driver verification

Drivers start with `PENDING` verification status. A trusted operator must provision the first administrator through direct, controlled database access; admin self-registration is intentionally disabled. After verifying the account, an operator can promote an existing account with a reviewed, one-time SQL operation:

```sql
UPDATE app_users SET role = 'ADMIN' WHERE email = 'trusted-admin@example.com';
```

An administrator can approve or reject a driver using `PUT /api/v1/admin/drivers/{driverId}/verification` with body `{"decision":"APPROVED"}`. Protect database access and never expose the role update through a public endpoint.

## Driver availability and location

Approved drivers go online with `PUT /api/v1/driver/availability` and body `{"available":true}`. Send coordinates using `PUT /api/v1/driver/location` and body `{"latitude":6.5244,"longitude":3.3792}`.

Riders can discover nearby drivers using `GET /api/v1/rider/nearby-drivers?latitude=6.5244&longitude=3.3792&radiusKm=5`. Only approved online drivers with fresh locations are returned; drivers with active rides are excluded.

## Ride lifecycle

### Rider requests a ride

```http
POST /api/v1/rider/rides
Authorization: Bearer <RIDER_TOKEN>
Content-Type: application/json

{
  "pickupLatitude": 6.5244,
  "pickupLongitude": 3.3792,
  "destinationLatitude": 6.5350,
  "destinationLongitude": 3.3840
}
```

The server calculates straight-line distance with the Haversine formula and estimates a fare. Initial defaults are a ₦500 base fare plus ₦150 per kilometre; configure them with `BASE_FARE_NAIRA` and `PER_KM_FARE_NAIRA`. These are development defaults, not a production pricing policy; straight-line distance is not road distance.

### Driver dispatch and lifecycle

- `GET /api/v1/driver/rides/requests` — view up to 50 pending requests ranked by straight-line distance from the driver's last known location. The service inspects up to 200 oldest pending rides before ranking; this is an initial bounded candidate strategy, not a global nearest-ride guarantee.
- `GET /api/v1/driver/rides/requests/stream` — authenticated Server-Sent Events stream. Connect with an HTTP client that supports SSE and send the driver's bearer token. New requests emit as `ride-request` events; `heartbeat` events keep the connection active.
- `POST /api/v1/driver/rides/{rideId}/accept` — accept one requested ride.
- `POST /api/v1/driver/rides/{rideId}/arriving` — mark arrival at pickup.
- `POST /api/v1/driver/rides/{rideId}/start` — start the trip after arrival.
- `POST /api/v1/driver/rides/{rideId}/complete` — complete the trip.
- `POST /api/v1/rider/rides/{rideId}/cancel` — cancel before the trip starts.
- `GET /api/v1/rider/rides` and `GET /api/v1/driver/rides` — list the latest 20 rides for the authenticated account.
- `GET /api/v1/rider/rides/{rideId}` — get a ride owned by the authenticated rider.

The SSE implementation polls the existing dispatch query every `DISPATCH_NOTIFICATION_POLL_MS` milliseconds (default 3000) and keeps subscriber state in application memory. This is a single-instance starting point: multiple app replicas can send duplicate events, and a restart drops connection state. Use a durable broker/outbox and shared subscription coordination before horizontally scaling it. Clients should reconnect and refresh the regular request queue after reconnecting.

Ride transitions are checked in the domain model and rows are locked during acceptance and state changes. Driver presence rows are also locked when accepting, preventing concurrent requests from assigning multiple active rides to the same driver. Road routing and production pricing still need work. Cancellation fees are currently a configurable policy: no fee if cancelled before a driver accepts; a fixed fee after acceptance or while the driver is arriving. Default is ₦300 and must be reviewed against local law, customer terms, and actual operating costs.

## Tests and load testing

The integration test uses Testcontainers to run migrations against a real PostgreSQL container. It requires Docker. The GitHub Actions runner normally provides Docker, but local runs need a working Docker daemon:

```bash
mvn --batch-mode verify
```

A k6 script is provided under `load-tests/ride-request.js`. Run it only against a staging environment with a dedicated rider token; it creates ride rows. Example:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e RIDER_TOKEN="$RIDER_TOKEN" -e RATE=10 -e DURATION=30s load-tests/ride-request.js
```

To test the 10,000 requests/minute target, set `RATE=167` after validating the lower rate and checking database cleanup/retention. The target is not a verified benchmark. Measure p95/p99 latency, error rate, CPU, memory, database query latency, and connection-pool saturation.

## Architecture and production checklist

Keep the current modular monolith until measurements justify splitting services. PostgreSQL is the source of truth. Evaluate PostGIS or Redis geospatial indexes for high-frequency location search only after benchmarking.

Before production, add rate limiting and idempotency; token refresh/revocation; structured logs and tracing; TLS and deployment secrets; payment reconciliation; road routing and fare policy; cancellation/timeout policy; and realistic load tests. Avoid increasing thread or database pool sizes blindly—excess concurrency can overload PostgreSQL.

## Paystack payments (NGN)

Payments are initialized only after a ride reaches `COMPLETED`. The backend derives the payable amount from the persisted final fare; clients cannot submit an amount. Paystack's API uses kobo, so ₦1.00 is sent as 100 minor units.

Configure these environment variables on the API server:

- `PAYSTACK_SECRET_KEY` — Paystack secret key. Keep it in the VPS secret manager/environment; never commit it or expose it to the frontend.
- `PAYSTACK_CALLBACK_URL` — optional absolute URL to which Paystack redirects after checkout.
- `PAYSTACK_BASE_URL` — optional override for testing; defaults to `https://api.paystack.co`.

### API flow

1. Rider initializes payment for a completed ride: `POST /api/v1/rider/rides/{rideId}/payment`.
2. The API returns `authorizationUrl`, `reference`, and amount. Redirect the rider to `authorizationUrl`.
3. Configure the Paystack dashboard webhook URL as `https://YOUR_API_HOST/api/v1/webhooks/paystack`.
4. Paystack's `charge.success` webhook is validated using HMAC-SHA512 over the **raw request body** and the `x-paystack-signature` header. The backend checks reference, amount, and currency before marking a payment successful.
5. The rider can reconcile/check a payment by calling `GET /api/v1/rider/payments/{reference}`; the backend verifies the transaction server-to-server with Paystack.

The webhook endpoint is public at the HTTP layer because Paystack cannot provide a rider JWT; the cryptographic signature is mandatory. Do not treat the browser callback as proof of payment. Successful webhook processing is idempotent for an existing payment reference.

### Current limitations

This is the first Paystack collection integration, not a complete settlement system. Payment collection currently happens after ride completion, with no charge for pre-trip cancellations. Scheduled reconciliation runs every 60 seconds by default and checks stale initialized/pending payments. Provider verification and checkout initialization are performed outside database transactions; only short state transitions hold database locks. Configure `PAYMENT_RECONCILIATION_DELAY_MS` and `PAYMENT_RECONCILIATION_INITIAL_DELAY_MS` as needed. The job is safe to run on one application instance; if multiple replicas are deployed, add a distributed claim/lease or PostgreSQL `SKIP LOCKED` work claiming before scaling the scheduler horizontally.\n\n### Refunds, disputes, and driver payouts\n\n- Admin full refund: `POST /api/v1/admin/payments/{paymentReference}/refund` with `{"reason":"Customer support approved refund"}`. This version deliberately supports full refunds only. Refund creation is ledgered before calling Paystack; inspect the Paystack dashboard and refund webhooks if the provider request times out, and do not blindly retry an ambiguous request.\n- Chargebacks: `charge.dispute.create` and `charge.dispute.resolve` webhooks are signature-verified and recorded on the payment; the payment moves to `CHARGEBACK` for operational review. Dispute evidence upload and automated resolution are not implemented.\n- Driver payout: `POST /api/v1/admin/rides/{rideId}/payout` with `{"recipientCode":"RCP_..."}`. It requires a completed ride and successful rider payment, calculates payout from the stored payment amount, and creates one payout ledger row per ride. The Paystack recipient must already be created and verified through your trusted onboarding process. Default platform commission is 20%, configurable with `PLATFORM_COMMISSION_PERCENT`; set this only after defining your commercial terms. Paystack `transfer.success`, `transfer.failed`, and related transfer events update the payout ledger.\n- Cancellation fee: `CANCEL_FEE_AFTER_ACCEPT_NAIRA` defaults to 300. A rider cancelling before driver acceptance pays no fee. Cancellation after acceptance or when the driver is arriving incurs the fixed fee; the cancellation response includes `cancellationFeeNaira`. Collect it using the existing checkout endpoint for that cancelled ride. A fee is not applied after a trip starts because that transition cannot be cancelled. Publish this policy in customer-facing terms before enabling it in production.\n\nThis remains an integration foundation, not a complete finance ledger. Before production, add refund/payout reconciliation jobs, idempotency keys for admin operations, a durable outbox or lease-based work claim for multi-replica scheduling, provider contract tests, dispute evidence workflows, and operational alerts. Test all flows with Paystack test keys before live collection or transfers. Never send API keys in chat or commit them to source control.
