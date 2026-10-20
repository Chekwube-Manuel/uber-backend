package com.chekwube.uber.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerReference = :reference")
    Optional<Payment> findByReferenceForUpdate(@Param("reference") String reference);

    Optional<Payment> findByProviderReferenceAndRiderUserId(String reference, UUID riderUserId);
    Optional<Payment> findTopByRideIdAndRiderUserIdOrderByCreatedAtDesc(UUID rideId, UUID riderUserId);
    boolean existsByRideIdAndStatus(UUID rideId, PaymentStatus status);
    List<Payment> findTop20ByRiderUserIdOrderByCreatedAtDesc(UUID riderUserId);
}