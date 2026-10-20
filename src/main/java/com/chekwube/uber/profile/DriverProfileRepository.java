package com.chekwube.uber.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, UUID userId);
    boolean existsByVehiclePlateAndUserIdNot(String vehiclePlate, UUID userId);
}
