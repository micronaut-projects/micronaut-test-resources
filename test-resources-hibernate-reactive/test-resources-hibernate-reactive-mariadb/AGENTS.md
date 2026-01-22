# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T13:13:56Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
MariaDB-specific provider module for Hibernate Reactive test resources in Micronaut. Automatically provisions MariaDB containers via Testcontainers for reactive JPA testing, ensuring non-blocking database interactions with Vert.x and Mutiny.

Distinguishes from MySQL by using dedicated MariaDB Docker images and configurations, supporting MariaDB-specific features like Galera clustering or advanced optimizers, while maintaining drop-in compatibility for most MySQL workloads.

## STRUCTURE
```
./test-resources-hibernate-reactive-mariadb/
├── src/main/java/io/micronaut/testresources/hibernate/reactive/mariadb/  # Provider implementation
│   └── HibernateReactiveMariaDBTestResourceProvider.java
├── src/main/resources/META-INF/services/                              # SPI registration
│   └── io.micronaut.testresources.core.TestResourcesResolver
├── src/test/groovy/io/micronaut/testresources/hibernate/reactive/mariadb/  # Spock tests
│   ├── StandaloneStartMariaDBTest.groovy
│   └── WithJdbcStartMariaDBTest.groovy
├── src/test/resources/                                                # Test configurations
│   ├── application-standalone.yml
│   ├── application-jdbc.yml
│   └── logback.xml
└── build.gradle                                                       # Module build script
```

## WHERE TO LOOK
- Core provider logic → src/main/java/.../HibernateReactiveMariaDBTestResourceProvider.java (extends abstract core for container creation)
- Test examples → src/test/groovy/... (demonstrates container startup and reactive repo usage)
- Dependencies → build.gradle (Testcontainers MariaDB, JDBC MariaDB integration)
- SPI registration → src/main/resources/META-INF/services/... (registers provider with test resources core)

## CODE MAP
Small Java/Groovy module integrating Testcontainers for MariaDB. Single provider class (~50 lines) with Spock tests (~40 lines each). No complex architecture; focuses on container provisioning.

## CONVENTIONS
- DB Properties: Use 'jpa.<datasource>.properties.hibernate.connection.db-type = "mariadb"' to trigger this provider. Resolved properties include url, username, password under hibernate-reactive.mariadb.*
- Container Config: Ephemeral ports via Testcontainers; default image 'mariadb' (customizable via test-resources-config)
- Reactive Setup: Configures Vert.x MySQL client for non-blocking ops; integrates with Hibernate Reactive's Mutiny API
- Naming: Simple name 'mariadb' for property resolution; display name 'MariaDB (Hibernate reactive)'
- Gradle: Applies 'io.micronaut.build.internal.hibernate-reactive-module' plugin; depends on JDBC MariaDB for shared config
- Reuse: Leverages existing 'datasources.<datasource>.*' if db-type matches, avoiding redundant container spins

## ANTI-PATTERNS (THIS MODULE)
- Do NOT use MySQL provider for MariaDB tests; subtle differences (e.g., reserved words, default behaviors) may cause failures
- Avoid hard-coded ports or credentials; rely on dynamic Testcontainers binding and property resolution
- Do NOT mix blocking JDBC with reactive Hibernate; ensure all ops use Mutiny/Vert.x for non-blocking
- Deprecated: Avoid Oracle-specific patterns here; this is MariaDB-only (use sibling modules for other DBs)
- Do not duplicate abstract provider logic; always extend from core for consistency

## UNIQUE STYLES
- Extends abstract provider from hibernate-reactive-core for shared reactive logic
- Spock tests validate standalone and JDBC-integrated modes
- Minimal codebase: Focuses solely on MariaDB container instantiation
- Distinction from MySQL: Separate image and provider to handle MariaDB forks (e.g., better open-source support, feature divergences post-MySQL 5.7)

## COMMANDS
```bash
cd ../test-resources-hibernate-reactive/test-resources-hibernate-reactive-mariadb
../../gradlew build     # Build this module
../../gradlew check     # Run tests
../../gradlew publishToMavenLocal  # Publish locally
```

## NOTES
- Part of hibernate-reactive aggregator; use with micronaut-test-resources-core for full functionality
- MariaDB vs MySQL: Choose this for MariaDB-specific testing (e.g., Aria engine, thread pooling); MySQL provider for Oracle MySQL compatibility
- Tests demonstrate reactive book repo with timeout handling
- No main app; library for test-time DB provisioning
- Ensure Docker available for Testcontainers