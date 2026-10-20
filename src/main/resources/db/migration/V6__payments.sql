CREATE TABLE payments (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES rides(id),
    rider_user_id UUID NOT NULL REFERENCES app_users(id),
    provider VARCHAR(24) NOT NULL CHECK (provider IN ('PAYSTACK')),
    provider_reference VARCHAR(100) NOT NULL UNIQUE,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'NGN'),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INITIALIZED', 'PENDING', 'SUCCESS', 'FAILED')),
    authorization_url TEXT,
    access_code VARCHAR(128),
    provider_transaction_id BIGINT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_ride_created ON payments (ride_id, created_at DESC);
CREATE INDEX idx_payments_rider_created ON payments (rider_user_id, created_at DESC);
CREATE INDEX idx_payments_reconciliation ON payments (status, updated_at)
    WHERE status IN ('INITIALIZED', 'PENDING');