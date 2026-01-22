# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources JDBC Oracle XE: Submodule providing Testcontainers-based automatic provisioning of Oracle XE database for JDBC tests in Micronaut applications.

**DEPRECATION NOTE**: This module is deprecated since version 2.4.0 and marked for removal. Users should migrate to the unified &#39;oracle&#39; provider (e.g., test-resources-jdbc-oracle) which handles multiple Oracle variants including XE, Free, and Test Pilot.

## STRUCTURE
```
./test-resources-jdbc/test-resources-jdbc-oracle-xe/
├── build.gradle                          # Module build script with dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/oracle/xe/OracleXETestResourceProvider.java  # Core provider implementation
│   │   └── resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver  # SPI service file
│   └── test/
│       ├── groovy/io/micronaut/testresources/jdbc/xe/  # Spock tests (StartOracleXETest.groovy, OracleATPTest.groovy, OracleXEBookRepository.groovy)
│       └── resources/                     # Test configurations (application-test.yml, application-prod.yml, logback.xml)
└── .settings/ .project .classpath        # Eclipse/IDE configuration files
```

Compact submodule with a single provider class extending shared JDBC abstractions.

## WHERE TO LOOK
- Provider logic → src/main/java/io/micronaut/testresources/oracle/xe/OracleXETestResourceProvider.java (handles container creation, property resolution, and deprecation annotation)
- Integration tests → src/test/groovy/io/micronaut/testresources/jdbc/xe/ (verifies container startup, JDBC connectivity, and repository interactions)
- Build config → build.gradle (depends on testcontainers.oracle-xe and ojdbc8)
- Shared JDBC base → ../test-resources-jdbc-core/src/main/java/io/micronaut/testresources/jdbc/AbstractJdbcTestResourceProvider.java

## CODE MAP
Small Java/Groovy module: Core Java class for resource provision (~80 lines), Spock tests for validation. No complex architecture; inherits from jdbc-core.

## CONVENTIONS
- Docker image: Defaults to &#39;gvenzl/oracle-xe:slim-faststart&#39; for Oracle Express Edition; configurable via properties.
- Property namespace: Resolves under &#39;jdbc.oracle-xe&#39; (e.g., url, username, password).
- Production detection: Skips provisioning if &#39;ocid&#39; property is set (indicating ATP/production environment).
- Extends AbstractJdbcTestResourceProvider for consistent JDBC handling, ephemeral ports, and dynamic credentials.

## ANTI-PATTERNS (THIS PROJECT)
- Using this module in new projects or continuing in existing ones; immediately migrate to &#39;oracle&#39; to avoid future breakage.
- Hard-coding container configurations; always use overridable properties for images/ports.
- Ignoring deprecation; builds should treat deprecations as errors where possible.

## UNIQUE STYLES
- Explicit @Deprecated annotation with since/forRemoval details in the provider class.
- Special OCID check to differentiate test vs. production Oracle setups.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-oracle-xe:build  # Build the module
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-oracle-xe:check  # Run tests
./gradlew :micronaut-test-resources-jdbc:test-resources-jdbc-oracle-xe:publishToMavenLocal  # Publish locally
```

## NOTES
- Aggregated under test-resources-jdbc; not standalone.
- Migration tip: Update test resource configs to use &#39;oracle&#39; instead of &#39;oracle-xe&#39;.
- Tests use Micronaut Data JDBC repositories for realistic scenarios.
- No AOT or custom source sets; standard structure.
