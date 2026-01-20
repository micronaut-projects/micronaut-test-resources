# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:05:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources R2DBC MSSQL: Submodule providing automatic provisioning of Microsoft SQL Server containers for reactive (R2DBC) database testing in Micronaut applications. Integrates with the core test resources SPI to expose r2dbc.* properties, reusing shared container logic from the JDBC MSSQL provider for efficiency.

## STRUCTURE
```
./test-resources-r2dbc/test-resources-r2dbc-mssql/
├── src/main/java/io/micronaut/testresources/r2dbc/mssql/
│   └── R2DBCMSSQLTestResourceProvider.java    # Main provider implementation
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
├── src/test/groovy/io/micronaut/testresources/r2dbc/mssql/
│   ├── ReactiveBookRepository.groovy         # Test repository interface
│   ├── StandaloneStartMSSQLTest.groovy       # Standalone R2DBC container test
│   └── WithJdbcStartMSSQLTest.groovy         # Coexistence with JDBC test
├── src/test/resources/
│   ├── application-standalone.yml             # Test config for standalone R2DBC
│   └── application-jdbc.yml                   # Test config for JDBC + R2DBC
├── build.gradle                               # Build configuration
└── .settings/                                 # Eclipse IDE settings
```

## WHERE TO LOOK
- Provider implementation → src/main/java/io/micronaut/testresources/r2dbc/mssql/R2DBCMSSQLTestResourceProvider.java
- SPI registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver
- Base abstract provider logic → test-resources-r2dbc-core/src/main/java/io/micronaut/testresources/r2dbc/core/AbstractR2DBCTestResourceProvider.java (parent class)
- Shared MSSQL container logic → test-resources-jdbc/test-resources-jdbc-mssql/src/main/java/io/micronaut/testresources/mssql/MSSQLTestResourceProvider.java
- Test examples → src/test/groovy/ (Spock tests verifying container startup and reactive operations)

## CODE MAP
- R2DBCMSSQLTestResourceProvider extends AbstractR2DBCTestResourceProvider<MSSQLServerContainer<?>> to handle MS SQL Server R2DBC containers.
- Uses Testcontainers MSSQLServerContainer and MSSQLR2DBCDatabaseContainer.getOptions() for connection factory options.
- Registers via SPI for automatic discovery and property resolution.
- Tests use Micronaut Data R2DBC repository to validate reactive database operations.

## CONVENTIONS
- R2DBC URL format follows standard: "r2dbc:mssql://{host}:{port}/{database}" (constructed from Testcontainers-exposed properties via ConnectionFactoryOptions).
- Properties exposed under r2dbc.datasources.* namespace (e.g., r2dbc.datasources.default.url, r2dbc.datasources.default.db-type: mssql).
- Reuses JDBC MSSQL container instance for efficiency when both JDBC and R2DBC access is needed.
- Test configurations specify db-type: mssql, schema-generate: CREATE_DROP, dialect: SQL_SERVER under r2dbc.datasources.default.
- Requires test-resources.containers.mssql.accept-license: true to accept MS SQL Server license terms.
- Package structure: io.micronaut.testresources.r2dbc.{db}.*

## ANTI-PATTERNS (THIS PROJECT)
- None identified in this submodule; follows established patterns from parent R2DBC modules.
- Avoid hard-coding ports or image names; delegate to shared providers and Testcontainers dynamic binding.

## UNIQUE STYLES
- Minimal implementation: extends abstract provider, delegates container creation to JDBC counterpart for shared logic.
- Tests demonstrate both standalone R2DBC and coexistence with JDBC datasources.
- Uses Spock for BDD-style test verification of container provisioning and reactive repository operations.

## COMMANDS
```bash
cd test-resources-r2dbc/test-resources-r2dbc-mssql
../../gradlew build              # Build the module
../../gradlew test                # Run tests (verifies container startup and reactive ops)
../../gradlew check               # Run all checks including tests
../../gradlew publishToMavenLocal # Publish locally for testing
```

## NOTES
- Simple wrapper around shared logic; core functionality in abstract parent and JDBC provider.
- Supports reactive database testing with automatic container lifecycle management.
- For new R2DBC DB support, follow this pattern: extend AbstractR2DBCTestResourceProvider, implement extractOptions(), and depend on JDBC counterpart if available.
