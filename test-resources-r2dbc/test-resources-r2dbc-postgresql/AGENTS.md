# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
This module, test-resources-r2dbc-postgresql, is part of the Micronaut Test Resources suite. It provides automatic provisioning of PostgreSQL databases for reactive (R2DBC) connections in Micronaut tests. Using Testcontainers, it spins up ephemeral PostgreSQL containers, exposing connection properties under the r2dbc.datasources.* namespace. This enables seamless integration for reactive database testing without manual setup.

## STRUCTURE
```
./test-resources-r2dbc-postgresql/
├── build.gradle                          # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/micronaut/testresources/r2dbc/postgres/
│   │   │       └── R2DBCPostgreSQLTestResourceProvider.java  # Core provider implementation
│   │   └── resources/
│   │       └── META-INF/services/io.micronaut.testresources.core.TestResourcesResolver  # Service loader
│   └── test/
│       ├── groovy/                   # Spock test specifications
│       └── resources/                # Test configuration files (e.g., application.yml)
└── .settings/                            # IDE-specific settings (e.g., Eclipse)
```

## WHERE TO LOOK
- Core resolution logic → src/main/java/io/micronaut/testresources/r2dbc/postgres/R2DBCPostgreSQLTestResourceProvider.java
- Base R2DBC abstractions → ../test-resources-r2dbc-core/src/main/java/io/micronaut/testresources/r2dbc/core/AbstractR2DBCTestResourceProvider.java
- Test suites for validation → src/test/groovy/io/micronaut/testresources/r2dbc/postgres/*
- Dependencies and build setup → build.gradle (includes io.micronaut.testresources:test-resources-jdbc-postgresql for container sharing)

## CODE MAP
- R2DBCPostgreSQLTestResourceProvider: Extends AbstractR2DBCTestResourceProvider; handles PostgreSQL-specific container creation and property resolution.
- Supports database types: "postgresql", "postgres", "pg".
- Integrates with JDBC PostgreSQL provider for container reuse to optimize resource usage.

## CONVENTIONS
- Property namespaces: All resolved properties are prefixed with r2dbc.datasources.{name}.* (e.g., r2dbc.datasources.default.url).
- URL format: Follows r2dbc:postgresql://host:port/database?options for reactive connections.
- Container management: Uses Testcontainers' PostgreSQLContainer with dynamic ports and reusable instances where possible.
- Configuration: Avoid mixing with JDBC namespaces (jdbc.datasources.*) to prevent driver conflicts; R2DBC properties are resolved separately.
- Gradle: Depends on test-resources-r2dbc-core and test-resources-jdbc-postgresql for shared logic.

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code ports (e.g., fixed 5432) for containers; always use ephemeral/random port bindings to avoid conflicts in parallel tests.
- Avoid direct instantiation of containers outside the provider; rely on the resolver SPI for consistency.
- Deprecated practices: Steer clear of legacy Oracle XE variants (not applicable here, but relevant in broader R2DBC modules); use unified providers.
- In tests, do not block reactive operations; ensure all interactions respect Micronaut's non-blocking nature.

## UNIQUE STYLES
- Deep integration with JDBC counterparts for efficiency (reuses existing PostgreSQL containers if JDBC is also requested).
- Heavy use of Spock for testing, with Groovy-based specs under src/test/groovy.
- Minimal codebase: Focuses on PostgreSQL specifics while delegating common R2DBC logic to the core module.

## COMMANDS
```bash
../gradlew :micronaut-test-resources-r2dbc-postgresql:build    # Build this module
../gradlew :micronaut-test-resources-r2dbc-postgresql:check    # Run tests
../gradlew :micronaut-test-resources-r2dbc-postgresql:publishToMavenLocal
# For integration: Include in your project's build.gradle dependencies
```

## NOTES
- This module is an aggregator child under test-resources-r2dbc, sharing patterns with other DB variants (e.g., MariaDB, Oracle).
- Performance tip: Container reuse reduces startup time in tests requesting both JDBC and R2DBC PostgreSQL.
- If customizing, extend the provider carefully to maintain compatibility with the Test Resources SPI.
