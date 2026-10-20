package com.chekwube.uber.ride;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RidePostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("APP_TOKEN_KEY", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RideRepository rides;

    @Test
    void migrationsAndRidePersistenceWorkAgainstPostgres() {
        UUID riderId = UUID.randomUUID();
        jdbc.update("INSERT INTO app_users (id, email, password_hash, role) VALUES (?, ?, ?, ?)",
            riderId, "rider-" + riderId + "@integration.test", "test-hash", "RIDER");

        Ride ride = new Ride(riderId, 6.5244, 3.3792, 6.5350, 3.3840,
            new BigDecimal("1.35"), new BigDecimal("702.50"));
        rides.saveAndFlush(ride);

        Ride persisted = rides.findById(ride.getId()).orElseThrow();
        assertEquals(riderId, persisted.getRiderUserId());
        assertEquals(RideStatus.REQUESTED, persisted.getStatus());
        assertEquals(new BigDecimal("702.50"), persisted.getEstimatedFareNaira());
        assertTrue(jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Long.class) >= 5);
    }
}
