# Testing Strategy & Implementation Guide

## Overview

This document describes the comprehensive testing strategy for the Filmer backend application, including Integration Tests, E2E Tests, and Contract Tests.

## Test Organization

```
src/test/java/com/filmer/
├── integration/                        # Integration Tests with Real Database
│   ├── BaseIntegrationTest.java        # Base class with Testcontainers setup
│   ├── TestDataManager.java            # Utility for managing test data
│   ├── repository/                     # Repository layer integration tests
│   │   ├── GenreRepositoryIntegrationTest.java
│   │   └── MovieRepositoryIntegrationTest.java
│   ├── service/                        # Service layer integration tests
│   │   └── GenreServiceIntegrationTest.java
│   └── controller/                     # Controller/HTTP endpoint tests
│       └── GenreControllerIntegrationTest.java
│
├── contract/                           # Contract Tests (Future APIs)
│   ├── SearchApiContractTest.java      # Search API specification
│   ├── AuthAndCartApiContractTest.java # Auth & Cart API specification
│   └── CheckoutAndOrdersApiContractTest.java  # Checkout & Orders API specification
│
└── service/                            # Unit Tests (Existing)
    ├── GenreServiceTest.java           # Mocked service tests
    └── MovieServiceTest.java           # Mocked service tests
```

## Test Categories

### 1. Integration Tests (`integration/` folder)

**Purpose**: Test the full application stack with a real database using Testcontainers.

**Characteristics**:
- Uses PostgreSQL Docker container for isolated test database
- No mocks for repositories (real database interactions)
- Tests the complete flow: Controller → Service → Repository → Database
- Each test is independent and can run in isolation
- Automatic cleanup between tests

**Running Integration Tests**:
```bash
# Run all integration tests
mvn test -Dgroups=integration

# Run specific integration test class
mvn test -Dtest=GenreRepositoryIntegrationTest

# Run specific test method
mvn test -Dtest=GenreRepositoryIntegrationTest#testSaveGenre
```

### 2. Contract Tests (`contract/` folder)

**Purpose**: Define the expected API behavior for future implementation (Phase 4).

**Characteristics**:
- Defines request/response contracts for upcoming APIs
- Documents API specifications
- Tests may fail until features are implemented
- Serves as specification and acceptance criteria
- No real implementation required yet

**Contract Test Categories**:
1. **SearchApiContractTest**: Movie search, filtering, sorting
2. **AuthAndCartApiContractTest**: Authentication and shopping cart
3. **CheckoutAndOrdersApiContractTest**: Checkout and order management

**Running Contract Tests**:
```bash
# Run all contract tests
mvn test -Dgroups=contract

# Run specific contract test
mvn test -Dtest=SearchApiContractTest
```

### 3. Unit Tests (`service/` folder - Existing)

**Purpose**: Test individual components in isolation with mocks.

**Characteristics**:
- All dependencies are mocked
- Fast execution
- Focus on business logic
- No database access

## Test Environment Setup

### Testcontainers Configuration

The `BaseIntegrationTest` class handles all Testcontainers setup:

```java
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
        // Dynamically configure Spring datasource properties
    }
}
```

**Benefits**:
- Container lifecycle is automatically managed
- Each test run gets a fresh, clean database
- No manual database setup required
- Compatible with CI/CD pipelines

### Test Database Initialization

The test database is automatically initialized with:
- Schema creation (from `test/init-db.sql`)
- Sample test data (genres, movies, stars, customers)
- All necessary indexes

**Location**: `src/test/resources/test/init-db.sql`

## Test Data Management

### TestDataManager Utility

Provides helper methods for test data management:

```java
@Autowired
private TestDataManager testDataManager;

@BeforeEach
void setup() {
    // Clear all test data
    testDataManager.clearAllData();
    
    // Reset to base test data
    testDataManager.resetToBaseTestData();
}
```

**Available Methods**:
- `clearAllData()`: Delete all records
- `resetToBaseTestData()`: Reset to initial seed data
- `getRecordCount(tableName)`: Get table row count
- `genreExists(name)`: Check if genre exists
- `getGenreIdByName(name)`: Get genre ID

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Category

```bash
# Integration tests only
mvn test -Dgroups=integration

# Contract tests only
mvn test -Dgroups=contract

# Unit tests only
mvn test -Dgroups=unit
```

### Run with Clean Database

```bash
# Clear Docker containers and run fresh
docker container rm -f filmer-postgres-test
mvn clean test
```

### Run Single Test Class

```bash
mvn test -Dtest=GenreRepositoryIntegrationTest
```

### Run With Coverage

```bash
mvn clean test jacoco:report
# Coverage report: target/site/jacoco/index.html
```

## Test Structure Best Practices

### 1. Clear Test Names

```java
@Test
@DisplayName("Should save a new genre and persist to database")
void testSaveGenre() { ... }
```

### 2. Arrange-Act-Assert (AAA) Pattern

```java
@Test
void testExample() {
    // ARRANGE: Set up test data
    Genre genre = new Genre();
    genre.setName("Action");
    
    // ACT: Execute the operation
    Genre saved = genreRepository.save(genre);
    
    // ASSERT: Verify the result
    assertThat(saved.getId()).isNotNull();
}
```

### 3. Proper Test Isolation

```java
@BeforeEach
void setup() {
    // Clean database before each test
    testDataManager.clearAllData();
}
```

### 4. Meaningful Assertions

```java
// Good: Clear intent and message
assertThat(genres).hasSize(3);
assertThat(genres).extracting("name").containsExactly("Action", "Drama", "Comedy");

// Avoid: Vague assertions
assertTrue(results > 0);  // What does > 0 mean?
```

## Contract Testing Guide

Contract tests define expected APIs for future development. They are structured to:

1. **Document the API Contract**: Request format, response structure
2. **Define Success Criteria**: HTTP status codes expected
3. **Be Graceful About Failures**: Tests gracefully handle NotImplemented endpoints
4. **Support TDD**: Tests drive development from the API contract

### Example Contract Test Structure

```java
@Test
@DisplayName("Contract: Search movies by title should return paginated results")
void testSearchMoviesByTitleContract() {
    // GIVEN: A search request
    String url = BASE_URL + "/movies?title=Batman&page=0&size=10";
    
    // EXPECTED CONTRACT:
    // Status: 200 OK
    // Response structure includes: movies, totalMovies, totalPages, hasNextPage
    
    try {
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        // Assert contract
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("movies", "totalMovies", "totalPages");
    } catch (Exception e) {
        // Expected if endpoint not yet implemented
    }
}
```

## Continuous Integration

### GitHub Actions / CI/CD Configuration

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: filmer_test
    
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      
      # Run all tests (integration + unit)
      - run: mvn clean test
      
      # Generate coverage report
      - run: mvn jacoco:report
      
      # Upload results
      - uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: target/surefire-reports
```

## Dependencies

The testing setup uses:

- **Testcontainers**: `1.19.7` - Docker container management for tests
- **JUnit 5**: From Spring Boot Test
- **AssertJ**: `3.24.1` - Fluent assertions
- **REST Assured**: `5.4.0` - HTTP API testing
- **Spring Boot Test**: Web testing with `TestRestTemplate`
- **Mockito**: Mocked unit tests (existing setup)

All dependencies are in `pom.xml` with `<scope>test</scope>`.

## Troubleshooting

### Docker Not Available

**Error**: `Cannot connect to Docker socket`

**Solution**: 
```bash
# Install Docker Desktop or Docker Engine
# Ensure Docker daemon is running
docker ps
```

### Port Conflicts

**Error**: `Port already in use`

**Solution**: Testcontainers automatically finds available ports. If issues persist:
```bash
# Kill existing containers
docker kill $(docker ps -q)
```

### Database Initialization Failed

**Error**: `init-db.sql not found`

**Solution**: Ensure file is at `src/test/resources/test/init-db.sql`

### Slow Tests

**Solution**:
```bash
# Run tests in parallel
mvn test -DparallelUnit=true
```

## Future Improvements

1. **E2E Tests**: Add Selenium/Cypress tests for frontend integration
2. **Performance Tests**: Load testing with JMeter
3. **Security Tests**: Add OAuth2/JWT token validation tests
4. **API Documentation**: Generate API docs from contract tests
5. **Coverage Goals**: Set and enforce code coverage targets (>80%)

## Resources

- [Testcontainers Documentation](https://testcontainers.com/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/assertj-core-highlights.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

## Questions?

For questions about the testing setup, refer to the test classes themselves - they contain detailed JavaDoc comments explaining each test's purpose and approach.
