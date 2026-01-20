# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:15:00Z  
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0  
Branch: kafka-native

## OVERVIEW
Test Resources R2DBC Pool: A submodule within the R2DBC aggregator that enables connection pooling for R2DBC test resources. It configures the R2DBC driver to "pool" instead of the direct database driver (e.g., "postgresql"), allowing the r2dbc-pool library to manage connections. This is useful for tests requiring pooled connections to simulate production-like behavior or handle higher concurrency.

## STRUCTURE
```
./test-resources-r2dbc/test-resources-r2dbc-pool/
├── src/main/java/io/micronaut/testresources/r2dbc/pool/
│   └── R2DBCPoolTestResourceProvider.java          # Main provider implementation
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
├── src/test/groovy/io/micronaut/testresources/r2dbc/pool/
│   ├── R2DBCPoolDisablingTest.groovy               # Test for disabling pool
│   ├── ReactiveBookRepository.groovy               # Test repository for reactive operations
│   ├── StandaloneStartPostgreSQLTest.groovy        # Standalone pool test with PostgreSQL
│   └── WithJdbcStartPostgreSQLTest.groovy          # Integrated pool + JDBC test
├── src/test/resources/
│   ├── application-standalone.yml                   # Config for standalone tests
│   ├── application-jdbc.yml                         # Config for JDBC + pool tests
│   └── application-r2dbc-pool-disabled.yml          # Config to disable pool
└── build.gradle                                     # Build script with r2dbc.pool dependency
```

## WHERE TO LOOK
- Main functionality → R2DBCPoolTestResourceProvider.java (resolves pool driver and protocol properties)
- Test examples → StandaloneStartPostgreSQLTest.groovy or WithJdbcStartPostgreSQLTest.groovy (demonstrates pool usage with containers)
- Build configuration → build.gradle (dependencies on r2dbc-pool library)
- SPI registration → META-INF/services/io.micronaut.testresources.core.TestResourcesResolver (enables provider discovery)

## CODE MAP
[Skipped - LSP not initialized]. Single-file provider extending ToggableTestResourcesResolver; resolves only two properties (options.driver="pool", options.protocol=lowercased dialect).

## CONVENTIONS
- Pool config properties: The module does not directly configure pool-specific properties (e.g., maxSize, minIdle) — these are set via standard r2dbc.datasources.{name}.options.* in application config. It only switches the driver to "pool" and sets protocol to the database type (e.g., "postgresql" for PostgreSQL dialect).
- Property namespace: Resolves under r2dbc.datasources.{name}.options.driver and options.protocol.
- Protocol derivation: Converts the dialect (e.g., POSTGRESQL) to lowercase without underscores (e.g., "postgresql").
- Driver setting: Always resolves to "pool" to enable r2dbc-pool.

## ANTI-PATTERNS (THIS PROJECT)
- Over-tuning for tests: Avoid setting excessively large pool sizes (e.g., maxSize=100) in test configs, as tests typically run sequentially and don't need production-scale pools — this wastes resources and slows startup. Stick to minimal defaults or small values (e.g., maxSize=2-5).
- Unnecessary pooling: Don't enable pool in simple unit tests or low-concurrency integration tests where direct connections suffice; use pool only when testing connection reuse or concurrency.
- Hard-coding pool configs in tests: Configure pool options via external config files (e.g., application-test.yml) rather than inline, to avoid test fragility and allow overrides.

## UNIQUE STYLES
- Minimal provider: Unlike DB-specific providers, this is a thin "driver switcher" that doesn't spawn containers — it assumes a base R2DBC provider (e.g., postgresql) is present.
- Togglable: Implements ToggableTestResourcesResolver, allowing selective disabling via config (e.g., application-r2dbc-pool-disabled.yml).
- Dialect-aware protocol: Dynamically sets protocol based on resolved dialect, ensuring compatibility with pooled connections.

## COMMANDS
```bash
cd test-resources-r2dbc/test-resources-r2dbc-pool
../../gradlew build    # Build the module
../../gradlew test     # Run pool-specific tests (verifies PostgreSQL container startup with pooling)
../../gradlew check    # Full checks including tests
```

## NOTES
- Depends on r2dbc-pool library for actual pooling logic; this module only integrates it into the test resources SPI.
- Tested primarily with PostgreSQL; reuses JDBC containers for efficiency.
- Enables reactive connection pooling in tests, useful for high-throughput scenarios or simulating real-world load.
- No additional properties beyond driver/protocol; pool tuning (e.g., timeouts, sizes) is left to application config.