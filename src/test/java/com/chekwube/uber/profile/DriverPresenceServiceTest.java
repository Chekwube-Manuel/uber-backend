package com.chekwube.uber.profile;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverPresenceServiceTest {
    @Test
    void unapprovedDriverCannotGoOnline() {
        DriverPresenceRepository presence = mock(DriverPresenceRepository.class);
        DriverProfileRepository profiles = mock(DriverProfileRepository.class);
        UUID id = UUID.randomUUID();
        DriverProfile profile = new DriverProfile(id, "Test Driver", "+2348012345678", "Toyota", "Corolla", "Silver", "ABC123XY");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));

        DriverPresenceService service = new DriverPresenceService(presence, profiles);
        assertThrows(ResponseStatusException.class, () -> service.setAvailability(id, true));
        verify(presence, never()).save(any());
    }

    @Test
    void approvedDriverCanGoOnline() {
        DriverPresenceRepository presence = mock(DriverPresenceRepository.class);
        DriverProfileRepository profiles = mock(DriverProfileRepository.class);
        UUID id = UUID.randomUUID();
        DriverProfile profile = new DriverProfile(id, "Test Driver", "+2348012345678", "Toyota", "Corolla", "Silver", "ABC123XY");
        profile.setVerificationStatus("APPROVED");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));
        when(presence.findById(id)).thenReturn(Optional.empty());
        when(presence.save(any(DriverPresence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverPresenceService service = new DriverPresenceService(presence, profiles);
        ProfileDtos.DriverAvailabilityResponse response = service.setAvailability(id, true);
        assertTrue(response.available());
    }
}