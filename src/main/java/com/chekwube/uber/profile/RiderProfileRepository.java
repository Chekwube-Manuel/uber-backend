package com.chekwube.uber.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RiderProfileRepository extends JpaRepository<RiderProfile, UUID> {
    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, UUID userId);
}
