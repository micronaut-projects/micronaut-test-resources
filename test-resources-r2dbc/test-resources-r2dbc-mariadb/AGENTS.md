# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:36:28Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources: R2DBC MariaDB provider, supplying reactive MariaDB databases via Testcontainers for Micronaut tests. Builds on shared R2DBC core to minimize duplication with similar providers like MySQL.

## STRUCTURE
```
./test-resources-r2dbc-mariadb/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   └── java/io/micronaut/testresources/r2dbc/mariadb/
│   │       └── R2DBCMariaDBTestResourceProvider.java  # Core provider implementation
│   └── test/                             # Tests for the provider
└── README.md                             # Module-specific documentation (if present)
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/r2dbc/mariadb/R2DBCMariaDBTestResourceProvider.java
- Shared R2DBC abstractions → ../test-resources-r2dbc-core/ (aggregator and base classes)
- Testcontainers integration → Inherited from test-resources-testcontainers

## CODE MAP
Small module focused on MariaDB-specific R2DBC provisioning. Extends AbstractR2DBCTestResourceProvider for shared behavior.

Key class: R2DBCMariaDBTestResourceProvider
- Implements TestResourcesResolver
- Uses MariaDBContainer for container management
- Extracts options via MariaDBR2DBCDatabaseContainer

## CONVENTIONS
- URL format: r2dbc:mariadb://{host}:{port}/{database}
- Properties namespace: r2dbc.datasources.* (e.g., url, username, password)
- Container image: &quot;mariadb&quot; (defaults to latest compatible)
- Ephemeral ports for container bindings
- Shares abstract base with MySQL provider to handle common R2DBC resolution, avoiding code duplication

## ANTI-PATTERNS (THIS MODULE)
- Do NOT duplicate MySQL provider code; extend shared abstract classes instead
- Avoid fixed ports; always use random/ephemeral assignments
- Do not pin specific MariaDB versions unless required; rely on Testcontainers defaults
- Preserve namespace consistency: stick to r2dbc:mariadb prefix, differentiate from jdbc:mariadb

## UNIQUE STYLES
- Minimalist submodule: Focuses solely on MariaDB adaptations
- Heavy reliance on inheritance from r2dbc-core for resolver SPI implementation

## COMMANDS
```bash
./gradlew :micronaut-test-resources-r2dbc-mariadb:build    # Build this module
./gradlew :micronaut-test-resources-r2dbc-mariadb:check    # Run tests
./gradlew :micronaut-test-resources-r2dbc-mariadb:publishToMavenLocal
```

## NOTES
- Differs from MySQL provider mainly in container class (MariaDBContainer vs MySQLContainer) and options extractor
- No Oracle XE equivalents; unified for MariaDB
- Integrates with overall test-resources ecosystem for seamless reactive DB testing
