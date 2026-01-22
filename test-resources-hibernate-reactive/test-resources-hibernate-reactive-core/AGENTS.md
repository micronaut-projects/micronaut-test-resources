# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Core module for Hibernate Reactive test resources in the Micronaut Test Resources suite. Provides shared abstractions and base provider classes for provisioning reactive database containers (via Testcontainers) for non-blocking JPA testing in Micronaut applications. Supports integration with Hibernate Reactive's Mutiny/Vert.x stack for databases like PostgreSQL, MySQL, etc.

This module serves as the foundation for database-specific submodules, handling common logic for reactive connection properties and container management.

## STRUCTURE
```
./test-resources-hibernate-reactive-core/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   └── java/io/micronaut/testresources/hibernate/reactive/core/
│   │       ├── AbstractHibernateReactiveTestResourceProvider.java  # Base provider class
│   │       └── HibernateReactiveSupport.java                       # Shared utilities
│   └── testFixtures/groovy/io/micronaut/testresources/hibernate/reactive/core/
│       ├── Book.groovy                   # Test entity example
│       └── BookRepository.groovy         # Test repository example
└── .settings/                            # IDE configuration files
```

## WHERE TO LOOK
- Base provider implementation → src/main/java/io/micronaut/testresources/hibernate/reactive/core/AbstractHibernateReactiveTestResourceProvider.java
- Shared support utilities → src/main/java/io/micronaut/testresources/hibernate/reactive/core/HibernateReactiveSupport.java
- Test fixtures for examples → src/testFixtures/groovy/io/micronaut/testresources/hibernate/reactive/core/
- Dependencies and build logic → build.gradle (depends on testcontainers-jdbc, micronaut-data-hibernate-reactive, etc.)

## CODE MAP
Java/Groovy codebase with a focus on extending Testcontainers for reactive databases. Key class: AbstractHibernateReactiveTestResourceProvider extends AbstractTestContainersProvider for container lifecycle management.

## CONVENTIONS
- **Reactive Props**: Exposes properties under 'jpa.<datasource>.properties.hibernate.connection.*' namespace (e.g., url, username, password, db-type). Providers automatically configure reactive session factories with Vert.x drivers.
- Property resolution: Integrates with core test-resources SPI, reusing 'datasources.<datasource>.*' if db-type matches.
- Container management: Uses ephemeral ports and dynamic binding; supports automatic startup/shutdown.
- Config requirements: Application config must include 'jpa' and 'datasources' entries with reactive=true implied.
- Build: Applies 'io.micronaut.build.internal.testcontainers-module' and 'io.micronaut.build.internal.test-fixtures' plugins.
- Testing: Uses Spock/Groovy for test fixtures; includes entity/repository examples for validation.

## ANTI-PATTERNS (THIS PROJECT)
- **Blocking JDBC**: Do not mix blocking JDBC operations (e.g., synchronous queries) in reactive contexts; always use reactive drivers and async patterns to avoid blocking the event loop.
- Hard-coded ports: Avoid specifying fixed ports for databases; rely on Testcontainers' random port assignment.
- Duplication: Do not replicate logic across DB providers; extend from AbstractHibernateReactiveTestResourceProvider for shared behavior.
- Sync calls in reactive tests: Preserve non-blocking nature; avoid synchronous methods that could stall Vert.x threads.
- Deprecated integrations: Ensure compatibility with unified 'oracle' providers instead of legacy XE variants.

## UNIQUE STYLES
- Extends aggregator pattern from parent test-resources-hibernate-reactive, with core providing base for per-DB extensions.
- Focus on reactive: All logic ensures Mutiny-based asynchronous operations.
- Test fixtures: Includes Groovy-based entity/repository for easy testing of reactive JPA setups.
- No runtime application; purely a test-time library module.

## COMMANDS
```bash
cd ../test-resources-hibernate-reactive/test-resources-hibernate-reactive-core
../../gradlew build    # Build the core module
../../gradlew check    # Run tests and validations
../../gradlew publishToMavenLocal  # Publish core artifact locally
```

## NOTES
- Depends on parent test-resources-core for Resolver SPI.
- Designed for databases supporting reactive drivers (e.g., PostgreSQL, MySQL).
- Integrates with Micronaut Data Hibernate Reactive for session management.
- Test fixtures demonstrate usage but are not published by default (disabled in build.gradle).
- For full aggregator context, see ../AGENTS.md.
