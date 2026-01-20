# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T13:45:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
This module provides test resources support for Hibernate Reactive with Oracle XE database, automatically provisioning an Oracle XE container via Testcontainers for Micronaut applications during testing.

**DEPRECATION NOTE**: This module is deprecated since version 2.4.0 and will be removed. Prefer the unified 'oracle' provider in test-resources-hibernate-reactive-oracle for all Oracle variants.

## STRUCTURE
```
./test-resources-hibernate-reactive/test-resources-hibernate-reactive-oracle-xe/
├── build.gradle                                  # Module build file
├── src/main/java/io/micronaut/testresources/hibernate/reactive/oracle/
│   └── HibernateReactiveOracleXETestResourceProvider.java  # Main provider class (deprecated)
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
├── src/test/groovy/io/micronaut/testresources/hibernate/reactive/oracle/
│   ├── StandaloneStartOracleXEDBTest.groovy      # Standalone test
│   └── WithJdbcStartOracleXEDBTest.groovy        # JDBC integration test
└── src/test/resources/
    ├── application-jdbc.yml                      # JDBC test config
    ├── application-standalone.yml                # Standalone test config
    └── logback.xml                               # Test logging config
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/hibernate/reactive/oracle/HibernateReactiveOracleXETestResourceProvider.java
- Extends AbstractHibernateReactiveTestResourceProvider for shared reactive Hibernate behavior
- Container config: Uses OracleContainer with default image 'gvenzl/oracle-xe:slim-faststart'
- Tests → src/test/groovy/* for verification of container startup and integration

## CODE MAP
Small Java module integrating Testcontainers for Oracle XE. Single provider class extending abstract base from hibernate-reactive-core. Groovy/Spock tests validate functionality.

## CONVENTIONS
- Property namespaces: Follows Hibernate Reactive patterns - jpa.*.properties.hibernate.connection.* (url, username, password)
- DB type: 'oracle-xe' (but deprecated; use 'oracle')
- Container management: Automatic startup with dynamic ports via Testcontainers
- Dependencies: Relies on micronaut-test-resources-jdbc-oracle-xe for JDBC base and testcontainers-oracle-xe
- Gradle: Applies 'io.micronaut.build.internal.hibernate-reactive-module' plugin
- Reactive setup: Configures Vert.x Oracle client for non-blocking operations

## ANTI-PATTERNS (THIS MODULE)
- Avoid using this deprecated module; migrate to unified 'oracle' provider to prevent future removal issues
- Do not hard-code database ports; rely on Testcontainers' ephemeral port binding
- Ensure non-blocking operations in reactive contexts; this provider handles Vert.x integration
- Do not duplicate logic; extend from hibernate-reactive-core abstract provider

## UNIQUE STYLES
- Deprecated XE-specific variant; part of Oracle family with free/xe distinctions (but prefer unified)
- Integrates with parent hibernate-reactive aggregator
- Uses Groovy/Spock for testing, consistent with project
- SPI registration for TestResourcesResolver

## COMMANDS
```bash
cd test-resources-hibernate-reactive/test-resources-hibernate-reactive-oracle-xe
../../../gradlew build    # Build module
../../../gradlew test    # Run tests
../../../gradlew publishToMavenLocal  # Publish locally
```

## NOTES
- Deprecated: Use test-resources-hibernate-reactive-oracle instead
- Supports Oracle XE slim image for lightweight testing
- Integrates with Micronaut's test resources SPI for automatic resolution
- No main application; library for test-time database provisioning
- Tests demonstrate standalone and JDBC-integrated usage