# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources JDBC Oracle Free: Specialized provider for automatic provisioning of Oracle Database Free Edition test containers using Testcontainers. Supports JDBC-based integration tests in Micronaut applications by spawning ephemeral containers and exposing dynamic configuration properties. This module unifies Oracle support under the 'oracle' db-type, replacing deprecated XE variants.

## STRUCTURE
```
./test-resources-jdbc-oracle-free/
├── build.gradle                          # Module build script with Testcontainers dependencies
├── src/main/java/io/micronaut/testresources/oracle/free/OracleFreeTestResourceProvider.java # Core provider implementation
├── src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver # Service loader for resolver discovery
├── src/test/groovy/io/micronaut/testresources/jdbc/free/ # Spock tests (e.g., StartOracleFreeTest, OracleATPTest)
└── src/test/resources/                   # Test configs (application-test.yml, logback.xml)
```
Small, focused submodule extending shared JDBC core logic for Oracle Free specifics.

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/oracle/free/OracleFreeTestResourceProvider.java (extends AbstractJdbcTestResourceProvider)
- Integration tests → src/test/groovy/io/micronaut/testresources/jdbc/free/StartOracleFreeTest.groovy (verifies container startup and JDBC ops)
- Production detection → Provider checks for 'ocid' property to skip in non-test environments
- Build and deps → build.gradle (includes testcontainers-oracle-free and ojdbc8)
- ATP simulation → src/test/groovy/io/micronaut/testresources/jdbc/free/OracleATPTest.groovy (mocks Autonomous Transaction Processing scenarios)

## CODE MAP
Skipped (small Java/Groovy module with single provider class and tests).

## CONVENTIONS
- Oracle Free image tag: Defaults to 'gvenzl/oracle-free:slim-faststart'; overridable via 'containers.oracle.image-name' property for custom images or versions.
- Service name: Relies on OracleContainer defaults (typically 'FREEPDB1' for Free Edition PDB); exposed in JDBC URL as 'jdbc:oracle:thin:@//host:port/service_name'.
- Properties: Resolves under 'datasources.{name}.*' namespace with db-type 'oracle' (e.g., url, username=system, password=OracleCo1, driver-class-name); requires 'ocid' check to disable in production; customizations via 'containers.oracle.*' (e.g., image-name, init-script-path). Ensures ephemeral ports and dynamic binding.

## ANTI-PATTERNS (THIS PROJECT)
- Hard-coding container details like ports or credentials; always use Testcontainers dynamic methods (e.g., container.getJdbcUrl(), getUsername()).
- Setting 'ocid' in test configs, as it signals production mode and disables the provider.
- Duplicating shared JDBC logic; always extend AbstractJdbcTestResourceProvider instead of reimplementing resolution or startup.
- Using fixed image tags without fallback; prefer configurable properties for flexibility.

## UNIQUE STYLES
- Unified 'oracle' naming for properties across Oracle variants (Free, XE, etc.) to simplify configuration.
- Production awareness via 'ocid' property to avoid test container usage in real environments.
- Spock tests inherit from AbstractJDBCSpec for consistent JDBC verification.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc-oracle-free:build  # Build the module
./gradlew :micronaut-test-resources-jdbc-oracle-free:check  # Run tests
./gradlew :micronaut-test-resources-jdbc-oracle-free:publishToMavenLocal # Publish locally
```

## NOTES
- Deprecation of XE modules: Oracle XE variants (e.g., test-resources-jdbc-oracle-xe) are deprecated; prefer this unified 'oracle' provider for JDBC, which supports Free Edition and aligns with R2DBC/Hibernate Reactive counterparts.
- Integrates with Testcontainers Oracle Free module; requires acceptance of Oracle license in some contexts.
- For ATP (Autonomous) scenarios, provider skips if 'ocid' is set, allowing real cloud resources.
