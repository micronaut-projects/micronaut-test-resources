# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources R2DBC Oracle XE: Submodule providing reactive test resources for Oracle XE databases using Testcontainers. 

**DEPRECATION NOTE**: This module is deprecated. Prefer the unified 'oracle' provider (e.g., test-resources-r2dbc-oracle-free) for Oracle support in R2DBC tests. The XE variant may be removed in future versions.

## STRUCTURE
```
./test-resources-r2dbc/test-resources-r2dbc-oracle-xe/
├── build.gradle                           # Module build configuration
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/r2dbc/oracle/  # Provider implementation
│   │   └── resources/META-INF/services/   # Service loader for resolver
│   └── test/
│       ├── groovy/io/micronaut/testresources/r2dbc/oracle/  # Spock tests
│       └── resources/                      # Test configurations (YAML, logback)
└── .settings/ .project .classpath         # Eclipse/IDE configs
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/r2dbc/oracle/R2DBCOracleXETestResourceProvider.java (extends AbstractR2DBCTestResourceProvider, handles Oracle XE container)
- Service registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver
- Test examples → src/test/groovy/io/micronaut/testresources/r2dbc/oracle/*Test.groovy (e.g., StandaloneStartOracleTest, OracleATPReactiveTest)
- Dependencies and description → build.gradle (depends on testcontainers.oracle.xe and micronaut-test-resources-jdbc-oracle-xe)

## CODE MAP
Skipped (LSP not initialized). Small Java/Groovy module focused on Oracle XE R2DBC provider with Spock tests.

## CONVENTIONS
- Follows R2DBC namespace: r2dbc.datasources.* properties (e.g., url, username, password).
- Reuses JDBC Oracle XE container for efficiency when both JDBC and R2DBC are needed.
- Uses Testcontainers with dynamic ports; container image: gvenzl/oracle-xe:slim-faststart.
- Tests use Spock with separate configs for JDBC/R2DBC (e.g., application-jdbc.yml).
- Properties resolved via ConnectionFactoryOptions for R2DBC URLs.

## ANTI-PATTERNS (THIS PROJECT)
- Avoid using this deprecated XE variant; migrate to unified 'oracle' for better compatibility and maintenance.
- Do not hard-code database ports or credentials; rely on Testcontainers' ephemeral setup.
- Mixing JDBC and R2DBC in same datasource config—use separate namespaces.
- Skipping verification of container startup in tests; always test happy/error paths.

## UNIQUE STYLES
- Depends on JDBC counterpart (micronaut-test-resources-jdbc-oracle-xe) for shared container logic.
- Deprecated annotation on provider class with note to use 'oracle'.
- Test resources exposed under clear R2DBC namespace.

## COMMANDS
```bash
cd test-resources-r2dbc/test-resources-r2dbc-oracle-xe
../../../gradlew build    # Build this module
../../../gradlew check    # Run tests
../../../gradlew publishToMavenLocal
```

## NOTES
- Integrates with Micronaut Test Resources SPI for automatic R2DBC property provisioning.
- For migration: Update dependencies to unified Oracle module and adjust db-type to 'oracle'.
- Limited to Oracle XE; use with caution due to deprecation.