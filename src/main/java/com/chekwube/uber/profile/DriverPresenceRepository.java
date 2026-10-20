package com.chekwube.uber.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DriverPresenceRepository extends JpaRepository<DriverPresence, UUID> {
    @Query(value = """
        SELECT p.*
        FROM driver_presence p
        JOIN driver_profiles d ON d.user_id = p.driver_user_id
        WHERE p.is_available = TRUE
          AND d.verification_status = 'APPROVED'
          AND p.latitude IS NOT NULL
          AND p.longitude IS NOT NULL
          AND p.last_location_at >= CURRENT_TIMESTAMP - INTERVAL '2 minutes'
          AND p.latitude BETWEEN :minLatitude AND :maxLatitude
          AND p.longitude BETWEEN :minLongitude AND :maxLongitude
          AND 6371.0 * acos(least(1.0, greatest(-1.0,
              cos(radians(:latitude)) * cos(radians(p.latitude)) *
              cos(radians(p.longitude) - radians(:longitude)) +
              sin(radians(:latitude)) * sin(radians(p.latitude))
          ))) <= :radiusKm
        ORDER BY 6371.0 * acos(least(1.0, greatest(-1.0,
              cos(radians(:latitude)) * cos(radians(p.latitude)) *
              cos(radians(p.longitude) - radians(:longitude)) +
              sin(radians(:latitude)) * sin(radians(p.latitude))
          )))
        LIMIT 100
        """, nativeQuery = true)
    List<DriverPresence> findNearbyAvailableDrivers(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radiusKm") double radiusKm,
        @Param("minLatitude") double minLatitude,
        @Param("maxLatitude") double maxLatitude,
        @Param("minLongitude") double minLongitude,
        @Param("maxLongitude") double maxLongitude
    );
}