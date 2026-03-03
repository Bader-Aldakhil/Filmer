package com.filmer.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * 
 * Provides:
 * - PostgreSQL Docker container (via Testcontainers)
 * - Dynamic property configuration for Spring Boot test context
 * - Automatic schema initialization
 * 
 * All integration tests should extend this class to ensure:
 * - Real database interactions (no mocks for repositories)
 * - Isolated test data
 * - Automatic container lifecycle management
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("filmer_test")
            .withUsername("filmer")
            .withPassword("filmer_test_password")
            .withInitScript("test/init-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @BeforeAll
    static void setupDatabase() {
        // Database is automatically initialized by Testcontainers
        // using the init script at src/test/resources/test/init-db.sql
    }
}
