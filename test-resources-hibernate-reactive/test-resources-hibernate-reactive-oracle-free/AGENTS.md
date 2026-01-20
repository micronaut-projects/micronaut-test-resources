# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T13:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Module providing Oracle Free Database support for Hibernate Reactive in Micronaut Test Resources.
Automatically provisions an Oracle Free test container via Testcontainers for reactive JPA testing.
Integrates with Micronaut's test resources SPI to supply database connection properties.

## STRUCTURE
```
./test-resources-hibernate-reactive-oracle-free/
├── build.gradle                          # Module build file with dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/hibernate/reactive/oracle/
│   │   │   └── HibernateReactiveOracleFreeTestResourceProvider.java  # Core provider class
│   │   └── resources/META-INF/services/
│   │       └── io.micronaut.testresources.core.TestResourcesResolver  # Service loader
│   └── test/
│       ├── groovy/io/micronaut/testresources/hibernate/reactive/oracle/
│       │   ├── StandaloneStartOracleFreeDBTest.groovy  # Standalone provider tests
│       │   └── WithJdbcStartOracleFreeDBTest.groovy    # JDBC-integrated tests
│       └── resources/
│           ├── application-jdbc.yml       # JDBC test config
│           ├── application-standalone.yml # Standalone test config
│           └── logback.xml                # Logging config for tests
└── .classpath, .project, etc.             # IDE config files
```

## WHERE TO LOOK
- Provider implementation → src/main/java/.../HibernateReactiveOracleFreeTestResourceProvider.java
  (Extends AbstractHibernateReactiveTestResourceProvider from core module)
- Test cases → src/test/groovy/.../*Test.groovy (Spock specs verifying container startup and properties)
- Build config → build.gradle (Dependencies: testcontainers.oracle.free, vertx.oracle, jdbc-oracle-free)
- Service registration → src/main/resources/META-INF/services/... (Registers the provider)

## CODE MAP
Small Java/Groovy module with single provider class and Spock tests. No complex architecture;
focuses on Oracle Free container creation and property resolution for Hibernate Reactive.

## CONVENTIONS
- Property matching: Uses 'oracle' as db-type in configs (e.g., datasources.default.db-type: oracle)
- Container image: Defaults to 'gvenzl/oracle-free:slim-faststart' (configurable via test resources)
- Reactive setup: Configures Vert.x Oracle driver for non-blocking operations
- Namespaces: jpa.<name>.properties.hibernate.connection.* (url, username, password)
- Ephemeral ports: Automatic random port binding via Testcontainers
- Gradle plugin: Applies 'io.micronaut.build.internal.hibernate-reactive-module' for build consistency

## ANTI-PATTERNS (THIS MODULE)
- Deprecated Oracle XE: Avoid using oracle-xe variants; this oracle-free module is the preferred replacement
- Do not hard-code ports or credentials; let Testcontainers handle dynamic provisioning
- Avoid blocking calls; ensure all operations align with Hibernate Reactive's async model

## UNIQUE STYLES
- Focused single-DB provider, unlike aggregator modules
- Depends on JDBC Oracle Free for base database support
- Tests cover both standalone HR and combined JDBC+HR scenarios
- Minimal codebase: ~50 lines in provider, emphasizing extension from core abstract class

## COMMANDS
```bash
cd test-resources-hibernate-reactive/test-resources-hibernate-reactive-oracle-free
../../../gradlew build              # Build module
../../../gradlew test               # Run Spock tests
../../../gradlew publishToMavenLocal # Publish to local Maven repo
../../../gradlew check              # Run checks including tests
```

## NOTES
- Part of test-resources-hibernate-reactive aggregator; not standalone
- Supports Oracle Free for cloud/native-friendly testing (lighter than full Oracle)
- Deprecation emphasis: XE is deprecated due to licensing/maintenance; migrate to this provider
- Integrates with Micronaut AOT for GraalVM compatibility in tests
- No main entrypoint; library module for test-time use only
