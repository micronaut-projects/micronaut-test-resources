# MODULE KNOWLEDGE BASE: test-resources-r2dbc-mysql

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
This submodule extends Micronaut Test Resources to provide reactive (R2DBC) test resources for MySQL databases.
It leverages Testcontainers to automatically provision MySQL containers during tests, enabling reactive database
interactions without manual setup. This module is part of the larger test-resources-r2dbc aggregator, focusing
specifically on MySQL support.

Key features:
- Automatic MySQL container management for R2DBC connections
- Dynamic property resolution for r2dbc.datasources.*
- Integration with Micronaut's test resources SPI for seamless usage in tests

## STRUCTURE
```
test-resources-r2dbc-mysql/
├── build.gradle                          # Gradle build configuration with dependencies
└── src/
    └── main/
        └── java/
            └── io/micronaut/testresources/r2dbc/mysql/
                └── R2DBCMySQLTestResourceProvider.java  # Core provider implementation
```

Note: This module has no dedicated tests or resources; it reuses from parent aggregators.

## WHERE TO LOOK
- **Provider Implementation**: R2DBCMySQLTestResourceProvider.java - Handles container creation, options extraction,
  and R2DBC URL construction. Extends AbstractR2DBCTestResourceProvider for shared logic.
- **Build Configuration**: build.gradle - Defines dependencies on testcontainers:mysql and reuses JDBC MySQL module.
  Applies 'io.micronaut.build.internal.r2dbc-module' plugin for consistent build setup.
- **Parent Integration**: Look in test-resources-r2dbc-core for base R2DBC abstractions and resolvers.
- **JDBC Reuse**: Container setup logic is inherited/reused from test-resources-jdbc-mysql.

For usage examples, check integration tests in the main project or aggregator modules.

## CONVENTIONS
- **R2DBC MySQL URL**: Follows the standard format &quot;r2dbc:mysql://{host}:{port}/{database}&quot;. The provider
  constructs this dynamically using container host, exposed port, and resolved database name. Avoid manual URL
  construction in tests; rely on resolved properties.
- **Image Tag**: Default Docker image is &quot;mysql:8.4.5&quot;. This can be overridden via test resources properties
  (e.g., micronaut.test.resources.mysql.image-name). Use semantic versioning for custom tags to ensure compatibility.
- **Property Namespaces**: Exposes properties under &quot;r2dbc.datasources.default.*&quot; (or named datasources).
  Common ones: url, username, password, driverClassName.
- **Container Configuration**: Uses ephemeral ports for MySQL (default 3306 internally). Database name, user, and
  password are dynamically set or resolved from test context.
- **Reuse Patterns**: Heavily reuses JDBC MySQL container setup to avoid duplication. Extend from core providers
  for any customizations.

## ANTI-PATTERNS (THIS MODULE)
- **Hardcoding Ports or URLs**: Never hardcode r2dbc:mysql URLs or ports in tests/providers; always use dynamic
  resolution to support local/remote modes and avoid conflicts.
- **Duplicating JDBC Logic**: Avoid reimplementing container creation or options; always extend/reuse from
  test-resources-jdbc-mysql to maintain consistency between JDBC and R2DBC.
- **Ignoring Reactive Constraints**: Do not use blocking operations in providers; ensure all logic is non-blocking
  to align with R2DBC's reactive nature.
- **Custom Image Misuse**: Don't use unsupported MySQL versions/tags without testing; stick to official mysql images
  and verify compatibility with R2DBC drivers.
- **Overriding Defaults Unnecessarily**: Resist changing default image or config unless required; it breaks
  portability across environments.

## UNIQUE STYLES
- Minimal codebase: Relies on inheritance from r2dbc-core and jdbc-mysql for most functionality.
- No module-specific tests: Validation occurs in aggregator or end-to-end tests.

## COMMANDS
```bash
# Build this module (from project root)
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-mysql:build

# Run checks
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-mysql:check
```

## NOTES
- This module is tightly coupled with test-resources-r2dbc-core; changes here may require updates there.
- Supports MySQL-specific features like reactive transactions, but defers to R2DBC driver for implementation.
- For GraalVM/native image support, ensure container images are compatible.
- Version: Aligns with parent project; check build.gradle for exact deps.

(End of knowledge base - 58 lines)
