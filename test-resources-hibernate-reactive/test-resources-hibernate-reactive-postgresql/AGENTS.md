# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Module providing PostgreSQL test container support for Hibernate Reactive in Micronaut. Automates reactive database provisioning using Testcontainers for non-blocking JPA testing.

## STRUCTURE
```
./test-resources-hibernate-reactive-postgresql/
├── src/main/java/io/micronaut/testresources/hibernate/reactive/postgresql/
│   └── HibernateReactivePostgreSQLTestResourceProvider.java  # Core provider class
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # Service registration
├── src/test/groovy/io/micronaut/testresources/hibernate/reactive/postgresql/
│   ├── StandaloneStartPostgreSQLDBTest.groovy  # Standalone container test
│   └── WithJdbcStartPostgreSQLDBTest.groovy    # JDBC-integrated test
├── src/test/resources/
│   ├── application-standalone.yml
│   ├── application-jdbc.yml
│   └── logback.xml
└── build.gradle  # Gradle build with dependencies
```

## WHERE TO LOOK
- Provider implementation → src/main/java/.../HibernateReactivePostgreSQLTestResourceProvider.java (extends core abstract, creates PostgreSQLContainer)
- Shared logic → test-resources-hibernate-reactive-core/.../AbstractHibernateReactiveTestResourceProvider.java
- Tests → src/test/groovy/... (Spock specs verifying container startup and properties)
- Build config → build.gradle (Testcontainers PostgreSQL, JDBC runtime dependency)

## CODE MAP
Compact Java/Groovy module. Single main class overrides container creation and defaults for PostgreSQL (image: 'postgres', simple name: 'postgres'). Tests in Spock.

## CONVENTIONS
- Properties: jpa.<datasource>.properties.hibernate.connection.* (url, username, password, db-type='postgres')
- Dynamic ports via Testcontainers for ephemeral bindings
- Reuses JDBC PostgreSQL if configured for migrations
- Reactive focus: Vert.x drivers, Mutiny sessions
- Gradle plugin: 'io.micronaut.build.internal.hibernate-reactive-module'
- Test resources SPI registration via META-INF/services

## ANTI-PATTERNS (THIS PROJECT)
- Do NOT hard-code fixed ports like 5432 for PostgreSQL; prefer Testcontainers' random port mapping
- Avoid mixing blocking JDBC in reactive contexts; use reactive drivers only
- Do not reimplement core provider logic; always extend AbstractHibernateReactiveTestResourceProvider
- Steer clear of sync operations that block Vert.x event loop

## UNIQUE STYLES
- Minimalist DB-specific impl, leveraging aggregator core for most functionality
- Runtime linkage to JDBC counterpart for hybrid setups
- Exclusive reactive Hibernate support (no blocking alternatives)

## COMMANDS
```bash
cd ../test-resources-hibernate-reactive-postgresql
../../gradlew build   # Build the module
../../gradlew check   # Run Spock tests
../../gradlew publishToMavenLocal  # Local publish
```

## NOTES
- Submodule of test-resources-hibernate-reactive aggregator
- Integrates with Micronaut test resources for automatic resolution
- Focuses on PostgreSQL for reactive JPA; see siblings for other DBs
- No entry point; used via test dependencies in apps