package com.chekwube.uber.ride;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);

    List<Ride> findTop50ByStatusAndDriverUserIdIsNullOrderByRequestedAtAsc(RideStatus status);

    List<Ride> findTop20ByRiderUserIdOrderByRequestedAtDesc(UUID riderUserId);

    Optional<Ride> findByIdAndRiderUserId(UUID id, UUID riderUserId);

    boolean existsByDriverUserIdAndStatusIn(UUID driverUserId, Collection<RideStatus> statuses);

    List<Ride> findTop20ByDriverUserIdOrderByRequestedAtDesc(UUID driverUserId);
}