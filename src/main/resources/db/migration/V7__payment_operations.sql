ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('INITIALIZED', 'PENDING', 'SUCCESS', 'FAILED', 'REFUND_PENDING', 'REFUNDED', 'CHARGEBACK'));
ALTER TABLE payments ADD COLUMN dispute_reference VARCHAR(100);
ALTER TABLE payments ADD COLUMN dispute_status VARCHAR(40);

CREATE TABLE payment_refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    provider_reference VARCHAR(100) NOT NULL UNIQUE,
    provider_refund_id BIGINT,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('INITIALIZED', 'PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'NEEDS_ATTENTION')),
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_payment_refunds_one_active ON payment_refunds(payment_id)
    WHERE status IN ('INITIALIZED', 'PENDING', 'PROCESSING', 'NEEDS_ATTENTION');
CREATE INDEX idx_payment_refunds_status_updated ON payment_refunds(status, updated_at);

CREATE TABLE driver_payouts (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE REFERENCES rides(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    driver_user_id UUID NOT NULL REFERENCES app_users(id),
    recipient_code VARCHAR(100) NOT NULL,
    provider_reference VARCHAR(100) NOT NULL UNIQUE,
    transfer_code VARCHAR(100),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'NGN'),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INITIALIZED', 'PENDING', 'SUCCESS', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_payouts_driver_created ON driver_payouts(driver_user_id, created_at DESC);
