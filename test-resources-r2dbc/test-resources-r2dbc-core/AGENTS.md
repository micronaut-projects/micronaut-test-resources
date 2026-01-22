# MODULE KNOWLEDGE BASE: test-resources-r2dbc-core

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Core module for R2DBC test resources in Micronaut Test Resources suite. Provides foundational abstractions and utilities for resolving reactive database connection properties (URL, username, password) using Testcontainers. Focuses on basic datasource resolution without connection pooling.

Distinct from the test-resources-r2dbc aggregator, which serves as a bundler for core, database-specific providers (e.g., MySQL, PostgreSQL), and pooling extensions. This core module contains shared logic reusable across all R2DBC implementations, emphasizing extensibility for custom providers.

## STRUCTURE
```
./test-resources-r2dbc/test-resources-r2dbc-core/
├── src/
│   └── main/
│       └── java/
│           └── io/
│               └── micronaut/
│                   └── testresources/
│                       └── r2dbc/
│                           └── core/
│                               ├── AbstractR2DBCTestResourceProvider.java
│                               └── R2dbcSupport.java
└── build.gradle  # Module configuration with dependencies on testcontainers and r2dbc-spi
```

## WHERE TO LOOK
- **Provider Backbone**: AbstractR2DBCTestResourceProvider - Abstract base class extending AbstractTestContainersProvider. Manages property resolution, container creation/reuse (prefers existing JDBC containers), and extraction of ConnectionFactoryOptions for R2DBC URLs.
- **Utility Helpers**: R2dbcSupport - Static utility class with constants (e.g., prefixes, required properties) and methods for parsing property expressions, extracting datasource names, and identifying resolvable/required properties.

For database-specific extensions, see sibling modules like test-resources-r2dbc-mysql.

## CODE MAP
Small codebase focused on two key classes:
- AbstractR2DBCTestResourceProvider: ~150 lines, handles resolution logic, container management, and abstract methods for DB types and options extraction.
- R2dbcSupport: ~100 lines, pure utilities for property handling without business logic.

No tests in this module; testing likely in aggregator or integration suites.

## CONVENTIONS
- **Property Naming**: All properties prefixed with 'r2dbc.datasources.{datasource-name}.' (e.g., r2dbc.datasources.default.url). Required keys include 'dialect', 'driverClassName', 'db-type'.
- **URL Formation**: Dynamically built from container details via ConnectionFactoryOptions: 'r2dbc:{driver}://{host}:{port}/{database}' (database optional). Ensures compatibility with R2DBC drivers.
- **Pool Interplay**: Core module does not implement pooling; it provides raw connection details. Pooling is handled in test-resources-r2dbc-pool via R2DBCPoolTestResourceProvider, which can wrap core providers for enhanced connection management. Use 'r2dbc.datasources.{name}.options.CONNECTION_FACTORY.pool.enabled' to trigger pooling.
- Follows core Test Resources SPI for resolvers, integrating seamlessly with Testcontainers for ephemeral containers.

## ANTI-PATTERNS (THIS MODULE)
- **Mixing JDBC/R2DBC Props**: Avoid blending jdbc.* and r2dbc.* in the same datasource config; they require different drivers and may lead to resolution conflicts or invalid URLs.
- **Fixed Ports**: Never hard-code ports; rely on Testcontainers' dynamic binding to prevent conflicts in parallel tests.
- **Unnecessary Container Creation**: Always check for reusable JDBC containers before spinning up new ones to optimize resource usage.
- **Ignoring Edge Cases**: Failing to handle container startup failures or invalid property expressions can lead to brittle tests; always include error paths in implementations.

## UNIQUE STYLES
- Emphasis on reuse: Prefers sharing containers across JDBC/R2DBC for efficiency.
- Static utilities in R2dbcSupport promote clean, functional-style property manipulation.
- Designed for extension: Abstract methods (e.g., getDbTypes(), extractOptions()) allow easy subclassing for new databases.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-core:build  # Build this module
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-core:check  # Run checks/tests (if any)
./gradlew publishToMavenLocal  # Publish locally for integration
```

## NOTES
- Module is lightweight (~250 LOC total), serving as a foundation for DB-specific providers.
- Depends on io.r2dbc:r2dbc-spi for ConnectionFactoryOptions.
- No direct entry points; used via Test Resources resolution chain.
- For pooling details, cross-reference test-resources-r2dbc-pool.
