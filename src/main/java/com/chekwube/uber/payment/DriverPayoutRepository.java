package com.chekwube.uber.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DriverPayoutRepository extends JpaRepository<DriverPayout, UUID> {
    Optional<DriverPayout> findByRideId(UUID rideId);
    Optional<DriverPayout> findByProviderReference(String reference);
}
