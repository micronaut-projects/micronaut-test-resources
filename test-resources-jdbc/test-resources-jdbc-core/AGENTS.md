# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Core module for JDBC test resources provisioning in Micronaut Test Resources suite.
This module provides the foundational abstract class and shared logic for starting
JDBC database containers using Testcontainers. It defines the SPI implementation
for resolving JDBC-related properties during tests.

Distinct from the test-resources-jdbc aggregator module, which serves as a
bundle for all database-specific providers (e.g., MySQL, PostgreSQL). This core
module focuses solely on the shared abstractions and base provider, without
including any specific database implementations.

Key features:
- Automatic resolution of datasource properties (URL, username, password, driver)
- Integration with Testcontainers for ephemeral database instances
- Support for multiple named datasources
- Extensible for various JDBC databases via subclassing

## STRUCTURE
```
./test-resources-jdbc-core/
├── src/
│   ├── main/
│   │   └── java/io/micronaut/testresources/jdbc/  # Core Java classes
│   └── testFixtures/
│       └── groovy/                                # Groovy test fixtures
└── build.gradle                                   # Gradle build configuration
```

## WHERE TO LOOK
- Base provider class: src/main/java/io/micronaut/testresources/jdbc/AbstractJdbcTestResourceProvider.java
  - Handles property resolution, container startup, and datasource config generation
- Test fixtures: src/testFixtures/groovy/io/micronaut/testresources/jdbc/AbstractJDBCSpec.groovy
  - Shared Spock specs for testing provider behavior
- Example entity: src/testFixtures/groovy/io/micronaut/testresources/jdbc/Book.groovy
  - Sample domain class used in tests

## CODE MAP
This is a small module with focused codebase:
- Single main class: AbstractJdbcTestResourceProvider (extends GenericContainerTestResourceProvider)
- Test fixtures in Groovy with Spock specifications
- No complex architecture; primarily an abstract base for extension in DB-specific modules

## CONVENTIONS
- Property namespaces: All resolved properties use 'datasources.{name}.*' pattern
  - Specific keys: url, username, password, driver-class-name
- Datasource generation:
  - URL: Dynamically from container.getJdbcUrl()
  - Username/Password: From container.getUsername() and getPassword()
  - Driver: Resolved based on database type
- Providers must override getDefaultImageName(), getJdbcPort(), getDriverClassName()
- Use Scope.any() for broad property resolution
- Containers use random ports; properties are populated post-startup

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code credentials: Always derive from Testcontainers methods
- Avoid fixed ports: Rely on Testcontainers' dynamic port binding
- Don't duplicate provider logic: Extend AbstractJdbcTestResourceProvider instead of reimplementing
- Avoid database-specific code here: Reserve for per-DB modules (e.g., test-resources-jdbc-mysql)
- Never use blocking operations in resolution logic

## UNIQUE STYLES
- Mix of Java for core logic and Groovy for tests/fixtures
- Heavy reliance on Testcontainers JDBC abstractions
- Minimal dependencies: Primarily testcontainers-jdbc and micronaut-test-resources-testcontainers
- No main application; purely a library module

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-core:build    # Build module
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-core:check    # Run tests
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-core:publishToMavenLocal
```

## NOTES
- This module is extended by specific database modules under test-resources-jdbc-*
- Ensure compatibility with Micronaut Data JDBC when adding new providers
- Tests use embedded H2 for verification, but providers support various DBs
- Part of larger JDBC aggregator; changes here affect all DB-specific providers
