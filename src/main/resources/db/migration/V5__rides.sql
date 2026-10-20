CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_user_id UUID NOT NULL REFERENCES app_users(id),
    driver_user_id UUID REFERENCES driver_profiles(user_id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'ACCEPTED', 'ARRIVING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    pickup_latitude DOUBLE PRECISION NOT NULL CHECK (pickup_latitude BETWEEN -90 AND 90),
    pickup_longitude DOUBLE PRECISION NOT NULL CHECK (pickup_longitude BETWEEN -180 AND 180),
    destination_latitude DOUBLE PRECISION NOT NULL CHECK (destination_latitude BETWEEN -90 AND 90),
    destination_longitude DOUBLE PRECISION NOT NULL CHECK (destination_longitude BETWEEN -180 AND 180),
    estimated_distance_km NUMERIC(10, 2) NOT NULL CHECK (estimated_distance_km > 0),
    estimated_fare_naira NUMERIC(12, 2) NOT NULL CHECK (estimated_fare_naira >= 0),
    final_fare_naira NUMERIC(12, 2),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ,
    arriving_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES app_users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rides_rider_requested ON rides (rider_user_id, requested_at DESC);
CREATE INDEX idx_rides_driver_active ON rides (driver_user_id, status)
    WHERE status IN ('ACCEPTED', 'ARRIVING', 'IN_PROGRESS');
CREATE INDEX idx_rides_dispatch_queue ON rides (requested_at)
    WHERE status = 'REQUESTED' AND driver_user_id IS NULL;