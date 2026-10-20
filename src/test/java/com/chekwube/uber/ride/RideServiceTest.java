package com.chekwube.uber.ride;

import com.chekwube.uber.profile.DriverPresenceRepository;
import com.chekwube.uber.profile.DriverProfileRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RideServiceTest {
    @Test
    void requestsRideWithServerCalculatedFare() {
        RideRepository rides = mock(RideRepository.class);
        RideService service = new RideService(rides, mock(DriverProfileRepository.class),
            mock(DriverPresenceRepository.class), new BigDecimal("500"), new BigDecimal("150"), new BigDecimal("300"));
        when(rides.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideDtos.RideResponse response = service.request(UUID.randomUUID(),
            new RideDtos.CreateRideRequest(6.5244, 3.3792, 6.535, 3.384));

        assertEquals(RideStatus.REQUESTED, response.status());
        assertTrue(response.estimatedDistanceKm().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.estimatedFareNaira().compareTo(new BigDecimal("500")) > 0);
        verify(rides).save(any(Ride.class));
    }

    @Test
    void cancellationFeeOnlyAppliesAfterDriverAcceptance() {
        UUID rider = UUID.randomUUID();
        Ride requested = new Ride(rider, 6.5, 3.3, 6.6, 3.4, new BigDecimal("12.00"), new BigDecimal("2300.00"));
        requested.cancel(rider, new BigDecimal("300.00"));
        assertEquals(BigDecimal.ZERO, requested.getCancellationFeeNaira());

        Ride accepted = new Ride(rider, 6.5, 3.3, 6.6, 3.4, new BigDecimal("12.00"), new BigDecimal("2300.00"));
        accepted.accept(UUID.randomUUID());
        accepted.cancel(rider, new BigDecimal("300.00"));
        assertEquals(new BigDecimal("300.00"), accepted.getCancellationFeeNaira());
    }

    @Test
    void rideCannotBeAcceptedTwice() {
        UUID rider = UUID.randomUUID();
        Ride ride = new Ride(rider, 6.5, 3.3, 6.6, 3.4, new BigDecimal("12.00"), new BigDecimal("2300.00"));
        UUID driver = UUID.randomUUID();
        ride.accept(driver);
        assertThrows(IllegalStateException.class, () -> ride.accept(UUID.randomUUID()));
    }

    @Test
    void cannotStartBeforeArriving() {
        Ride ride = new Ride(UUID.randomUUID(), 6.5, 3.3, 6.6, 3.4, new BigDecimal("12.00"), new BigDecimal("2300.00"));
        assertThrows(IllegalStateException.class, () -> ride.start(UUID.randomUUID()));
    }

    @Test
    void onlyAssignedDriverCanCompleteRide() {
        UUID driver = UUID.randomUUID();
        Ride ride = new Ride(UUID.randomUUID(), 6.5, 3.3, 6.6, 3.4, new BigDecimal("12.00"), new BigDecimal("2300.00"));
        ride.accept(driver);
        ride.markArriving(driver);
        ride.start(driver);
        assertThrows(IllegalStateException.class, () -> ride.complete(UUID.randomUUID()));
        ride.complete(driver);
        assertEquals(RideStatus.COMPLETED, ride.getStatus());
        assertEquals(new BigDecimal("2300.00"), ride.getFinalFareNaira());
    }
}