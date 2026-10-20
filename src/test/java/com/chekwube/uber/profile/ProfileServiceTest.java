package com.chekwube.uber.profile;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    @Test
    void createsRiderProfileForAuthenticatedUser() {
        RiderProfileRepository repository = mock(RiderProfileRepository.class);
        UUID userId = UUID.randomUUID();
        when(repository.existsByPhoneNumberAndUserIdNot("+2348012345678", userId)).thenReturn(false);
        when(repository.findById(userId)).thenReturn(Optional.empty());
        when(repository.save(any(RiderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RiderProfileService service = new RiderProfileService(repository);

        ProfileDtos.RiderProfileResponse response = service.upsert(userId, new ProfileDtos.RiderProfileRequest("Test Rider", "+2348012345678"));

        assertEquals(userId, response.userId());
        assertEquals("Test Rider", response.fullName());
        verify(repository).save(any(RiderProfile.class));
    }

    @Test
    void rejectsPhoneNumberAlreadyUsedByAnotherRider() {
        RiderProfileRepository repository = mock(RiderProfileRepository.class);
        UUID userId = UUID.randomUUID();
        when(repository.existsByPhoneNumberAndUserIdNot("+2348012345678", userId)).thenReturn(true);
        RiderProfileService service = new RiderProfileService(repository);
        assertThrows(ResponseStatusException.class, () -> service.upsert(userId, new ProfileDtos.RiderProfileRequest("Test Rider", "+2348012345678")));
        verify(repository, never()).save(any());
    }

    @Test
    void newDriverProfileStartsPendingVerification() {
        DriverProfile profile = new DriverProfile(UUID.randomUUID(), "Test Driver", "+2348012345678", "Toyota", "Corolla", "Silver", "abc 123xy");
        assertEquals("PENDING", profile.getVerificationStatus());
        assertEquals("ABC 123XY", profile.getVehiclePlate());
    }
}
