# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources JDBC Aggregator: Suite of Testcontainers-based providers for automatic JDBC database resources in tests.

## STRUCTURE
```
./test-resources-jdbc/
├── test-resources-jdbc-core/             # Shared abstractions and base JDBC provider
├── test-resources-jdbc-mysql/            # MySQL-specific provider
├── test-resources-jdbc-postgresql/       # PostgreSQL-specific provider
├── test-resources-jdbc-mariadb/          # MariaDB-specific provider
├── test-resources-jdbc-mssql/            # Microsoft SQL Server-specific provider
├── test-resources-jdbc-oracle-free/      # Oracle Free-specific provider
├── test-resources-jdbc-oracle-xe/        # Oracle XE-specific provider
├── test-resources-jdbc-oracle-test-pilot/# Oracle Test Pilot-specific provider
└── build.gradle                          # Aggregator build script (if present)
```
Aggregator organizing core logic and per-database submodules into categories: one core submodule for shared functionality and multiple per-DB submodules for specific implementations.

## WHERE TO LOOK
- Core provider logic → test-resources-jdbc-core/src/main/java/io/micronaut/testresources/jdbc/AbstractJdbcTestResourceProvider.java
- Per-DB modules → Each submodule's src/main/java/io/micronaut/testresources/{db}/...TestResourceProvider.java (e.g., MySQLTestResourceProvider in mysql submodule)
- Test fixtures and examples → test-resources-jdbc-core/src/testFixtures/groovy/io/micronaut/testresources/jdbc/AbstractJDBCSpec.groovy
- Build configuration → Submodule build.gradle files for dependencies like specific Testcontainers modules

## CODE MAP
Skipped (LSP not initialized). Modular Java/Groovy codebase with shared core and database-specific extensions.

## CONVENTIONS
- Property keys: Resolvers expose properties under \"datasources.{name}.*\" namespace (e.g., url, username, password, driver-class-name); customizations via \"containers.{db-type}.*\" (e.g., image-name, init-script-path)
- Image selection: Defaults to official Testcontainers images (e.g., mysql:8.0); overridable via properties for version or custom images; ensures ephemeral ports
- Provider extension: All DB providers extend AbstractJdbcTestResourceProvider for consistent resolution and container startup
- Testing: Uses Spock for integration tests verifying container startup and JDBC connectivity

## ANTI-PATTERNS (THIS PROJECT)
- Hard-coded credentials: Avoid directly returning fixed usernames/passwords; always use container.getUsername() and container.getPassword() for dynamic values (violation observed in oracle-test-pilot)
- Fixed ports: Do not hard-code container ports; rely on Testcontainers' random port binding
- Duplicate logic: Per-DB modules should not reimplement shared functionality; extend core abstract class instead

## UNIQUE STYLES
- Aggregator pattern with \"core\" submodule providing base classes reused across DB variants
- Heavy use of Testcontainers generics for type-safe container creation
- Spock-based tests with shared AbstractJDBCSpec for consistent verification across submodules

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc:build     # Build all JDBC submodules
./gradlew :micronaut-test-resources-jdbc:check     # Run tests across JDBC modules
```

## NOTES
- This is aggregator-level guidance; for per-database specifics, consult individual submodule sources
- Oracle variants handle special cases like license acceptance and ATP detection
- Integration with Micronaut Data JDBC for repository testing in examples
