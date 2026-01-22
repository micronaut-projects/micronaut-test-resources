# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources R2DBC Oracle Free: Module providing reactive test resources for Oracle Free database using Testcontainers.

## STRUCTURE
```
./test-resources-r2dbc/test-resources-r2dbc-oracle-free/
├── build.gradle                           # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/r2dbc/oracle/  # Provider implementation
│   │   └── resources/META-INF/services/   # Service loader for resolver
│   └── test/
│       ├── groovy/io/micronaut/testresources/r2dbc/oracle/  # Spock integration tests
│       └── resources/                     # Test application.yml configs
└── bin/                                   # Compiled classes (ignored)
```

## WHERE TO LOOK
- Core provider class → src/main/java/io/micronaut/testresources/r2dbc/oracle/R2DBCOracleFreeTestResourceProvider.java
- Service definition → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver
- Tests for standalone/reused containers → src/test/groovy/io/micronaut/testresources/r2dbc/oracle/StandaloneStartOracleTest.groovy and WithJdbcStartOracleTest.groovy
- ATP simulation test → src/test/groovy/io/micronaut/testresources/r2dbc/oracle/OracleATPReactiveTest.groovy
- Build config → build.gradle (depends on JDBC Oracle Free and Testcontainers Oracle)

## CODE MAP
[Skipped - Small module]. Extends AbstractR2DBCTestResourceProvider to handle Oracle Free container, options extraction, and property resolution.

## CONVENTIONS
- Oracle Free R2DBC connection uses ConnectionFactoryOptions (not string URL): DRIVER=\"oracle\", HOST=container.getHost(), PORT=container.getOraclePort(), DATABASE=container.getDatabaseName(), USER/PASSWORD from container.
- Exposed properties: r2dbc.datasources.<name>.url (resolved to options string), username, password, etc.
- Reuses JDBC Oracle Free container for efficiency if jdbc counterpart is requested.
- Default image: \"gvenzl/oracle-free:slim-faststart\"
- Skips provisioning if \"ocid\" property is set (indicates production env).
- Simple name for resolution: \"oracle\"

## ANTI-PATTERNS (THIS PROJECT)
- Avoid using deprecated Oracle XE modules (test-resources-r2dbc-oracle-xe); always prefer this unified \"oracle\" provider.
- Do not hard-code database ports or credentials; rely on Testcontainers dynamic allocation.
- Never mix JDBC and R2DBC configs in same datasource; use separate jdbc.* and r2dbc.* namespaces.
- Avoid blocking operations in reactive tests; ensure proper async handling.

## UNIQUE STYLES
- Module depends on JDBC Oracle Free for container sharing, reducing overhead.
- Tests use Spock with multiple profiles (standalone, with-jdbc, prod) to verify resolution.
- Supports ATP-like configs by checking for \"ocid\" to bypass container start.

## COMMANDS
```bash
# From project root
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-oracle-free:build   # Build module
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-oracle-free:check  # Run tests
./gradlew :micronaut-test-resources-r2dbc:test-resources-r2dbc-oracle-free:publishToMavenLocal
```

## NOTES
- XE deprecation: Oracle XE variants are deprecated across JDBC/R2DBC/Hibernate Reactive; migrate to this Oracle Free module for better support and unified naming (\"oracle\").
- Integrates with Micronaut Test Resources core to automatically provide r2dbc.* properties for Oracle.
- For custom images, override via test-resources config.
- Tests include reactive repository examples (e.g., ReactiveBookRepository) to verify connectivity.
