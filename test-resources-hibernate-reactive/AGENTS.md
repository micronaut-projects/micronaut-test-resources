# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Aggregator module for Hibernate Reactive test resources, providing automatic database container provisioning for reactive JPA testing in Micronaut applications.

## STRUCTURE
```
./test-resources-hibernate-reactive/
├── test-resources-hibernate-reactive-core/       # Shared abstractions and base provider
├── test-resources-hibernate-reactive-mysql/      # MySQL-specific provider
├── test-resources-hibernate-reactive-mariadb/    # MariaDB-specific provider
├── test-resources-hibernate-reactive-postgresql/ # PostgreSQL-specific provider
├── test-resources-hibernate-reactive-mssql/      # Microsoft SQL Server provider
├── test-resources-hibernate-reactive-oracle-free/# Oracle Free Edition provider (preferred for Oracle)
├── test-resources-hibernate-reactive-oracle-xe/  # Oracle XE provider (deprecated, use oracle-free or unified 'oracle')
└── build.gradle                                  # Aggregator build file including all submodules
```

## WHERE TO LOOK
- Core resolution logic → test-resources-hibernate-reactive-core/src/main/java/io/micronaut/testresources/hibernate/reactive/core/AbstractHibernateReactiveTestResourceProvider.java
- Shared utilities and constants → test-resources-hibernate-reactive-core/src/main/java/io/micronaut/testresources/hibernate/reactive/core/HibernateReactiveSupport.java
- Database-specific providers → Each submodule's src/main/java/io/micronaut/testresources/hibernate/reactive/<db>/HibernateReactive<DB>TestResourceProvider.java (e.g., PostgreSQL in postgresql submodule)
- Test fixtures and examples → Submodule test directories (e.g., test-resources-hibernate-reactive-core/src/test/groovy)

## CODE MAP
(Generated via LSP - assuming similar to parent project) Multi-module Gradle setup with Java/Groovy sources, focusing on Testcontainers integration for reactive databases.

## CONVENTIONS
- Property namespaces: jpa.<datasource>.properties.hibernate.connection.* (url, username, password, db-type)
- Reactive session factory: Providers configure Vert.x-based reactive drivers and Mutiny session factories for non-blocking operations
- DB settings: Automatic container startup with ephemeral ports; reuses existing 'datasources.<datasource>.*' if matching db-type
- Required config: Entries under 'jpa' and 'datasources' in application config
- Provider matching: By db-type (e.g., 'postgres', 'mysql') specified in properties
- Gradle: Each submodule applies 'io.micronaut.build.internal.hibernate-reactive-module' plugin for consistent build setup

## ANTI-PATTERNS (THIS PROJECT)
- Do NOT use blocking JDBC operations in reactive contexts; always prefer reactive drivers and async patterns
- Avoid hard-coding database ports; use Testcontainers' dynamic port binding
- Deprecated Oracle variants: oracle-xe is deprecated; prefer oracle-free or unified 'oracle' providers
- Do not duplicate per-DB logic; extend from core abstract provider for shared behavior
- Preserve non-blocking event loop in reactive tests; avoid sync calls that could block Vert.x threads

## UNIQUE STYLES
- Aggregator pattern with core + per-DB submodules, similar to JDBC/R2DBC siblings
- Heavy use of Testcontainers for DB provisioning, integrated with Micronaut's test resources SPI
- Spock/Groovy tests predominant in src/test/groovy
- Properties follow Micronaut's hierarchical config (jpa.default.reactive=true implied)

## COMMANDS
```bash
cd test-resources-hibernate-reactive
../gradlew build    # Build all HR submodules
../gradlew check    # Run tests for HR providers
../gradlew publishToMavenLocal  # Publish HR artifacts locally
```

## NOTES
- Integrates with parent test-resources-core for resolution SPI
- Supports MySQL, MariaDB, PostgreSQL, MSSQL, Oracle (with deprecation notes)
- Focus on reactive: All providers ensure compatibility with Hibernate Reactive's Mutiny/Vert.x stack
- No main application; pure library modules for test-time usage
- Oracle context: oracle-free/xe deprecated; prefer unified 'oracle' naming in properties