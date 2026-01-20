# MODULE KNOWLEDGE BASE: test-resources-hibernate-reactive-mysql

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
This module is part of Micronaut Test Resources, specifically providing a resolver for Hibernate Reactive with MySQL databases.
It uses Testcontainers to automatically provision MySQL containers (default image: mysql:8.4.5) for reactive JPA testing.
The provider extends core abstractions to resolve properties like connection URLs, usernames, passwords, and db-type under the 'jpa' namespace.
It supports reusing existing JDBC datasources if the db-type matches 'mysql', promoting efficient test setups without redundant containers.
Focuses on non-blocking operations via Vert.x drivers and Mutiny session factories.

## STRUCTURE
```
./test-resources-hibernate-reactive-mysql/
├── build.gradle                                  # Build config with dependencies (Testcontainers MySQL, JDBC MySQL)
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/hibernate/reactive/mysql/  # Provider implementation
│   │   │   └── HibernateReactiveMySQLTestResourceProvider.java
│   │   └── resources/META-INF/services/          # SPI service file for resolver registration
│   │       └── io.micronaut.testresources.core.TestResourcesResolver
│   └── test/
│       ├── groovy/io/micronaut/testresources/hibernate/reactive/mysql/  # Spock tests
│       │   ├── StandaloneStartMySQLDBTest.groovy  # Standalone container startup test
│       │   └── WithJdbcStartMySQLDBTest.groovy    # Test with JDBC datasource reuse
│       └── resources/                            # Test application configs
│           ├── application-jdbc.yml
│           ├── application-standalone.yml
│           └── logback.xml
└── .settings/ .project .classpath                # IDE config files (Eclipse/Buildship)
```

## WHERE TO LOOK
- Main provider logic: src/main/java/io/micronaut/testresources/hibernate/reactive/mysql/HibernateReactiveMySQLTestResourceProvider.java (extends AbstractHibernateReactiveTestResourceProvider)
- Container creation: Overrides createContainer to instantiate MySQLContainer
- Shared utilities: ../test-resources-hibernate-reactive-core/src/main/java/io/micronaut/testresources/hibernate/reactive/core/HibernateReactiveSupport.java (property resolution helpers)
- Core abstract class: ../test-resources-hibernate-reactive-core/src/main/java/io/micronaut/testresources/hibernate/reactive/core/AbstractHibernateReactiveTestResourceProvider.java (base resolution and container management)
- Test examples: src/test/groovy/* (demonstrates config and entity/repository usage)
- Build and deps: build.gradle (plugins, dependencies like vertx-mysql, mysql-connector-java)

## CONVENTIONS (DB PROPS)
- Namespace: jpa.<datasource-name>.properties.hibernate.connection.* (e.g., jpa.default.properties.hibernate.connection.url)
- Key properties: url, username, password, db-type (must be 'mysql' for this provider)
- db-type matching: Set datasources.<name>.db-type = 'mysql' to trigger resolution
- Reactive setup: Automatically configures hibernate.reactive = true, dialect = MySQLDialect, and Vert.x connection pool
- Reuse logic: If JDBC 'datasources.<name>.*' exists with matching db-type, reuses its URL/username/password instead of starting new container
- Ephemeral ports: Containers use random ports; properties are dynamically resolved and injected
- Config requirement: application.yml must define jpa and datasources sections for resolution to activate

## ANTI-PATTERNS
- Hard-coding ports or connection details in tests; always let Testcontainers handle dynamic binding
- Mixing blocking JDBC calls in reactive Hibernate contexts; can block Vert.x threads and degrade performance
- Overriding default image ('mysql:8.4.5') without strong justification; may break compatibility
- Duplicating code from core abstract provider; extend and override only what's DB-specific
- Ignoring db-type in config; leads to fallback to wrong provider or failure to resolve
- Using sync operations in reactive tests; preserve non-blocking nature

## UNIQUE STYLES
- Minimalist provider: Short class focused on MySQL specifics, delegating to core
- Spock tests: Groovy-based with data-driven examples for standalone/JDBC modes
- Dependency on JDBC counterpart: runtimeOnly(project(':micronaut-test-resources-jdbc-mysql')) for reuse

## COMMANDS
```bash
cd ../..  # To project root
./gradlew :micronaut-test-resources-hibernate-reactive:test-resources-hibernate-reactive-mysql:build  # Build module
./gradlew :micronaut-test-resources-hibernate-reactive:test-resources-hibernate-reactive-mysql:check  # Run tests
./gradlew publishToMavenLocal  # Publish locally (includes this module)
```

## NOTES
- Deprecated patterns: Align with parent project (e.g., prefer unified Oracle providers if applicable)
- Integration: Registers via META-INF/services for automatic discovery in test resources framework
- Tests cover entity persistence with reactive repositories, verifying container startup and property injection
- Small module: ~50 LOC in provider; relies heavily on hibernate-reactive-core for logic
