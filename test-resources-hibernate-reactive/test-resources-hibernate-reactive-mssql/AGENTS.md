# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
This module provides Microsoft SQL Server (MSSQL) database support for Micronaut Test Resources using Hibernate Reactive. It leverages Testcontainers to automatically provision and manage MSSQL containers for reactive JPA testing in Micronaut applications.

Key features:
- Automatic startup of MSSQL containers via Testcontainers
- Configuration of reactive connection properties for Hibernate Reactive
- Integration with Micronaut's test resources resolution SPI
- Supports ephemeral ports and container reuse

## STRUCTURE
```
./test-resources-hibernate-reactive-mssql/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/hibernate/reactive/mssql/
│   │   │   └── HibernateReactiveMSSQLTestResourceProvider.java  # Main provider class
│   │   └── resources/META-INF/services/
│   │       └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
│   └── test/
│       ├── groovy/io/micronaut/testresources/hibernate/reactive/mssql/
│       │   ├── StandaloneStartMSSQLDBTest.groovy     # Standalone container tests
│       │   └── WithJdbcStartMSSQLDBTest.groovy       # JDBC integration tests
│       └── resources/
│           ├── application-standalone.yml            # Test config for standalone mode
│           ├── application-jdbc.yml                  # Test config for JDBC mode
│           └── logback.xml                           # Logging configuration
└── .settings/ .project .classpath                # IDE configuration files
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/hibernate/reactive/mssql/HibernateReactiveMSSQLTestResourceProvider.java (extends AbstractHibernateReactiveTestResourceProvider)
- Test suites → src/test/groovy/io/micronaut/testresources/hibernate/reactive/mssql/*Test.groovy (Spock tests for container startup and integration)
- Dependencies and build → build.gradle (depends on testcontainers-mssql and micronaut-test-resources-jdbc-mssql)
- SPI service file → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CODE MAP
Small single-purpose Java/Groovy module (~5 source files). Java for provider implementation, Groovy/Spock for tests. Focuses on MSSQL-specific container configuration extending shared Hibernate Reactive core.

## CONVENTIONS
- Property namespaces: Follows parent (jpa.<datasource>.properties.hibernate.connection.*) with MSSQL-specific db-type 'mssql'
- Container management: Uses Testcontainers' MSSQLServerContainer with dynamic ports; reuses jdbc.mssql.* properties if available
- Reactive setup: Configures Vert.x MSSQL client for non-blocking operations
- Testing: Spock specs in src/test/groovy; uses Micronaut test annotations
- Gradle: Applies 'io.micronaut.build.internal.hibernate-reactive-module' plugin
- Config: Expects 'jpa.default.reactive=true' and matching db-type in application config

## ANTI-PATTERNS (THIS PROJECT)
- Do NOT hard-code database ports; rely on Testcontainers' getMappedPort()
- Avoid blocking calls in reactive contexts; use Mutiny/Vert.x async patterns
- Do not duplicate JDBC logic; extend from jdbc-mssql module where possible
- Preserve container lifecycle: Ensure proper shutdown to avoid resource leaks in tests
- No direct SQL execution in providers; focus on property resolution only

## UNIQUE STYLES
- MSSQL-specific: Handles Microsoft SQL Server quirks like default schema and authentication
- Minimal codebase: Primarily an extension of hibernate-reactive-core with DB-specific container init
- Test-driven: Comprehensive Spock tests for standalone and integrated scenarios
- No main app; library module for test-time usage only

## COMMANDS
```bash
cd test-resources-hibernate-reactive/test-resources-hibernate-reactive-mssql
../../gradlew build    # Build this module
../../gradlew check    # Run tests
../../gradlew publishToMavenLocal  # Publish artifact locally
```

## NOTES
- Part of hibernate-reactive aggregator; build from parent for full context
- Requires MSSQL JDBC and Vert.x MSSQL client at runtime
- Integrates with test-resources-jdbc-mssql for shared JDBC properties
- Supports Micronaut's test resources client/server modes
- Container image: Uses official mcr.microsoft.com/mssql/server from Testcontainers