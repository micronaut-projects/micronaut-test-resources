# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: [current commit]
Branch: [current branch]

## OVERVIEW
Micronaut Test Resources R2DBC: Aggregator module providing reactive database test resources via Testcontainers for various R2DBC drivers.

## STRUCTURE
```
./test-resources-r2dbc/
├── test-resources-r2dbc-core/             # Core abstractions and base R2DBC provider
├── test-resources-r2dbc-mariadb/          # MariaDB-specific R2DBC support
├── test-resources-r2dbc-mysql/            # MySQL-specific R2DBC support
├── test-resources-r2dbc-mssql/            # Microsoft SQL Server R2DBC support
├── test-resources-r2dbc-postgresql/       # PostgreSQL R2DBC support
├── test-resources-r2dbc-oracle-free/      # Oracle Free edition R2DBC support
├── test-resources-r2dbc-oracle-xe/        # Oracle XE edition R2DBC support (deprecated, prefer unified oracle)
├── test-resources-r2dbc-pool/             # R2DBC connection pool support
└── build.gradle                           # Aggregator build configuration
```

## WHERE TO LOOK
- Core provider logic → test-resources-r2dbc-core/src/main/java/io/micronaut/testresources/r2dbc/core/AbstractR2DBCTestResourceProvider.java
- Utility helpers → test-resources-r2dbc-core/src/main/java/io/micronaut/testresources/r2dbc/core/R2dbcSupport.java
- Per-DB modules (e.g., MySQL) → test-resources-r2dbc-mysql/src/main/java/io/micronaut/testresources/r2dbc/mysql/R2DBCMySQLTestResourceProvider.java
- Pool provider → test-resources-r2dbc-pool/src/main/java/io/micronaut/testresources/r2dbc/pool/R2DBCPoolTestResourceProvider.java
- Test examples → Each submodule's src/test/groovy/ (e.g., StandaloneStart*Test.groovy for container startup verification)

## CODE MAP
[Skipped - Use LSP for navigation]. This is a modular Java/Groovy codebase with per-database implementations extending the core abstract provider.

## CONVENTIONS
- R2DBC URL formation follows the standard scheme: "r2dbc:{driver}://{host}:{port}/{database}" (or without /database if null), built from Testcontainers-exposed properties via ConnectionFactoryOptions in AbstractR2DBCTestResourceProvider.
- Properties exposed under r2dbc.datasources.* namespace (e.g., r2dbc.datasources.default.url).
- Reuses JDBC containers when possible for efficiency (e.g., same database instance for JDBC and R2DBC access).
- Test configurations separate JDBC and R2DBC sections (e.g., application-jdbc.yml vs. r2dbc properties).

## ANTI-PATTERNS (THIS PROJECT)
- Avoid mixing JDBC and R2DBC configurations in the same datasource block, as they may require different drivers/URLs; use separate namespaces (jdbc.* vs r2dbc.*).
- Do not hard-code ports; rely on Testcontainers' dynamic port binding.
- Deprecated Oracle XE variant; use unified 'oracle' providers instead.
- Ensure tests verify both happy paths and error cases, like container startup failures.

## UNIQUE STYLES
- Each per-DB module depends on its JDBC counterpart for container reuse.
- Heavy use of Spock for testing container provisioning and property resolution.
- Properties follow clear namespaces: r2dbc.datasources.{name}.* 

## COMMANDS
```bash
cd test-resources-r2dbc
../gradlew build    # Build all R2DBC submodules
../gradlew check    # Run tests across submodules
../gradlew publishToMavenLocal
```

## NOTES
- Integrates with core test-resources SPI to provide r2dbc.* properties automatically.
- Supports connection pooling via r2dbc-pool module.
- For new DB support, extend AbstractR2DBCTestResourceProvider and implement extractOptions() for custom ConnectionFactoryOptions.
