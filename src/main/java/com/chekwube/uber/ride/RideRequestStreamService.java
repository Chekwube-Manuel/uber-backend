package com.chekwube.uber.ride;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RideRequestStreamService {
    private final RideService rides;
    private final ConcurrentHashMap<UUID, Subscription> subscriptions = new ConcurrentHashMap<>();

    public RideRequestStreamService(RideService rides) {
        this.rides = rides;
    }

    public SseEmitter subscribe(UUID driverId) {
        SseEmitter emitter = new SseEmitter(0L);
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription(driverId, emitter);
        subscriptions.put(subscriptionId, subscription);
        Runnable remove = () -> subscriptions.remove(subscriptionId);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("Ride request stream connected"));
        } catch (IOException exception) {
            remove.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @Scheduled(fixedDelayString = "${app.dispatch.notification-poll-ms:3000}")
    public void publishPendingRideRequests() {
        subscriptions.forEach((subscriptionId, subscription) -> {
            try {
                var pending = rides.dispatchQueue(subscription.driverId);
                for (RideDtos.RideResponse ride : pending) {
                    if (subscription.seenRideIds.add(ride.id())) {
                        subscription.emitter.send(SseEmitter.event()
                            .id(ride.id().toString())
                            .name("ride-request")
                            .data(ride));
                    }
                }
                subscription.emitter.send(SseEmitter.event().name("heartbeat").data("ok"));
            } catch (Exception exception) {
                subscriptions.remove(subscriptionId);
                subscription.emitter.completeWithError(exception);
            }
        });
    }

    private static final class Subscription {
        private final UUID driverId;
        private final SseEmitter emitter;
        private final Set<UUID> seenRideIds = ConcurrentHashMap.newKeySet();

        private Subscription(UUID driverId, SseEmitter emitter) {
            this.driverId = driverId;
            this.emitter = emitter;
        }
    }
}
