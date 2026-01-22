# PROJECT KNOWLEDGE BASE FOR test-resources-jdbc-postgresql

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
The test-resources-jdbc-postgresql module provides Testcontainers-based support for automatically provisioning PostgreSQL databases in Micronaut tests. It is part of the test-resources-jdbc aggregator, enabling ephemeral PostgreSQL instances for JDBC testing without manual configuration. This module resolves properties like JDBC URLs, usernames, and passwords dynamically from running containers.

## STRUCTURE
```
test-resources-jdbc/test-resources-jdbc-postgresql/
├── build.gradle                          # Gradle build configuration
├── src/main/java/io/micronaut/testresources/postgres/  # Provider implementation
│   └── PostgreSQLTestResourceProvider.java
├── src/main/resources/META-INF/services/ # SPI registration
│   └── io.micronaut.testresources.core.TestResourcesResolver
├── src/test/groovy/                      # Spock tests
│   └── io/micronaut/testresources/jdbc/mysql/  # Note: Path suggests some MySQL test artifacts; verify
│       ├── StartPostgreSQLTest.groovy
│       └── PostgreSQLBookRepository.groovy
├── src/test/resources/                   # Test config
│   ├── application-test.yml
│   └── logback.xml
└── .settings/ .project .classpath        # IDE configs
```

## WHERE TO LOOK
- **Provider Logic**: src/main/java/io/micronaut/testresources/postgres/PostgreSQLTestResourceProvider.java for PostgreSQL-specific resolution.
- **Base JDBC Logic**: Inherited from test-resources-jdbc-core/src/main/java/io/micronaut/testresources/jdbc/AbstractJdbcTestResourceProvider.java.
- **Tests**: src/test/groovy/ for integration tests verifying container startup and JDBC connectivity.
- **SPI Config**: src/main/resources/META-INF/services/ for resolver registration.
- **Build & Deps**: build.gradle for dependencies like testcontainers-postgresql.

## CODE MAP
- PostgreSQLTestResourceProvider: Extends AbstractJdbcTestResourceProvider; defines DB types ("postgresql", "postgres", "pg"), default image ("postgres").
- AbstractJdbcTestResourceProvider (from core): Handles property resolution, container creation, and configuration.

## CONVENTIONS
- **Postgres Image**: Uses "postgres" as default Docker image (official Testcontainers PostgreSQL image). Customizable via `containers.postgres.image-name` property in test resources config.
- **Properties**: Resolves under `datasources.{name}.*` namespace (e.g., url, username, password, driver-class-name). Specific props include:
  - `containers.postgres.db-name`: Database name.
  - `containers.postgres.username`: Overrides default username.
  - `containers.postgres.password`: Overrides default password.
  - `containers.postgres.init-script-path`: Path to initialization SQL script.
- Properties are dynamically set from container (e.g., ephemeral ports, generated credentials). Follows project-wide namespace conventions (jdbc.*, containers.*).

## ANTI-PATTERNS (THIS MODULE)
- **Fixed Port 5432**: Avoided; no occurrences in code. Uses Testcontainers' random port binding to prevent conflicts.
- Do NOT hard-code container configs; use resolvable properties and overrides.
- Avoid duplicating core logic; extend AbstractJdbcTestResourceProvider for consistency.

## UNIQUE STYLES
- Minimal code due to inheritance from jdbc-core; focuses only on PostgreSQL specifics.
- Supports multiple DB type aliases for flexible configuration.
- Tests use Spock/Groovy, aligning with project conventions.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-postgresql:build  # Build module
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-postgresql:test   # Run tests
./gradlew :micronaut-test-resources-jdbc:build                                # Build aggregator
./gradlew publishToMavenLocal                                       # Publish locally
```

## NOTES
- Module is lightweight (one main class); most functionality from jdbc-core.
- Verify test paths (e.g., mysql in postgresql tests? Possible misnomer or copy-paste).
- For usage: Set `datasources.default.db-type=postgres` in application.yml for automatic provisioning.
- No blocking ops; compatible with Micronaut's reactive style.
