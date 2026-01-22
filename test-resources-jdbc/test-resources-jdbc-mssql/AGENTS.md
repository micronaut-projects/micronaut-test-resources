# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T13:00:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources JDBC MSSQL: Testcontainers-based provider for automatic Microsoft SQL Server test container provisioning in JDBC tests.

## STRUCTURE
```
./test-resources-jdbc/test-resources-jdbc-mssql/
├── src/main/java/io/micronaut/testresources/mssql/MSSQLTestResourceProvider.java  # Main provider implementation
├── src/test/groovy/io/micronaut/testresources/mssql/MSSQLTestResourceProviderSpec.groovy  # Spock integration tests
├── src/test/resources/container-license-acceptance.txt  # License acceptance file for image
├── build.gradle  # Module build with Testcontainers MSSQL dep
└── src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
```
Small, focused submodule extending JDBC core for MSSQL-specific container setup and license handling.

## WHERE TO LOOK
- Provider logic → src/main/java/io/micronaut/testresources/mssql/MSSQLTestResourceProvider.java (extends AbstractJdbcTestResourceProvider)
- License handling → createMSSQLContainer method with acceptLicense() and LicenseAcceptance.assertLicenseAccepted
- Tests → MSSQLTestResourceProviderSpec.groovy for container startup and JDBC connectivity verification
- Dependencies → build.gradle for Testcontainers MSSQL module

## CONVENTIONS
- Docker image: mcr.microsoft.com/mssql/server:2022-latest (default, configurable via properties)
- License acceptance: Set 'test-resources.containers.mssql.accept-license=true' to accept EULA; otherwise requires manual acceptance via Testcontainers
- Credentials: SA_PASSWORD handled internally by Testcontainers MSSQLServerContainer (not exposed in code)
- Ports: Ephemeral/random binding (no fixed 1433)
- Properties: Exposes datasources.* under "mssql" namespace (url, username, password, driver-class-name)

## ANTI-PATTERNS (THIS PROJECT)
- Avoid hard-coding ACCEPT_EULA=yes in code; use license acceptance property instead
- Do not expose or hard-code SA_PASSWORD; rely on container.getPassword()
- Never bind to fixed port 1433; ensure ephemeral ports for concurrent test runs

## UNIQUE STYLES
- License acceptance via Micronaut property with fallback to Testcontainers global acceptance
- Static factory method createMSSQLContainer for reusable container creation
- Integration with Micronaut's JDBC namespace for seamless test resource injection

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc-mssql:build    # Build MSSQL provider
./gradlew :micronaut-test-resources-jdbc-mssql:test     # Run Spock tests
```

## NOTES
- Extends AbstractJdbcTestResourceProvider for consistent JDBC behavior across DB types
- License acceptance file lists image for reference; actual acceptance via property or global Testcontainers config
- No custom SA_PASSWORD setup; Testcontainers handles default SA user and password generation
- Suitable for integration with Micronaut Data JDBC repositories in tests
