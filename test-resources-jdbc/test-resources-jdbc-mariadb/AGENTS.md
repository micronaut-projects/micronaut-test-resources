# PROJECT KNOWLEDGE BASE

Generated: Tue Jan 20 2026
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources MariaDB Provider: Testcontainers-based module for automatic MariaDB database provisioning in Micronaut tests. Extends JDBC core abstractions for seamless integration without MySQL-specific features.

## STRUCTURE
```
./test-resources-jdbc/test-resources-jdbc-mariadb/
├── src/main/java/io/micronaut/testresources/mariadb/  # Provider implementation
├── src/main/resources/META-INF/services/              # Service loader config
├── src/test/groovy/io/micronaut/testresources/jdbc/mariadb/  # Integration tests
├── src/test/resources/                               # Test configs and init scripts
└── build.gradle                                      # Module build with dependencies
```

Compact submodule focused on MariaDB container handling, part of the JDBC aggregator.

## WHERE TO LOOK
- Core provider class → src/main/java/io/micronaut/testresources/mariadb/MariaDBTestResourceProvider.java (extends AbstractJdbcTestResourceProvider)
- Integration tests → src/test/groovy/.../StartMariaDBTest.groovy and StartMariaDBWithCustomizationsTest.groovy
- Build config → build.gradle (depends on testcontainers-mariadb and mariadb-java-client)
- Shared JDBC logic → Refer to test-resources-jdbc-core for base abstractions

## CODE MAP
Small module with one main Java class for provider logic, Spock tests in Groovy, and minimal resources. No complex architecture; inherits from JDBC core.

## CONVENTIONS
- MariaDB image tag: Defaults to 'mariadb' (e.g., mariadb:10.3 or latest); override with 'containers.mariadb.image-name' property for custom tags/versions.
- Properties: Exposes JDBC props under 'datasources.{name}.*' namespace (url, username, password, driver-class-name); activates on 'db-type' or 'dialect' = 'mariadb'.
- Container setup: Uses Testcontainers MariaDBContainer with random ports, dynamic credentials; supports customizations like init-script-path.
- Naming: Follows JDBC aggregator patterns without adding MySQL-like extras (e.g., no x-protocol-url).

## ANTI-PATTERNS (THIS PROJECT)
- Do not hard-code image tags or versions; always allow property-based overrides for flexibility.
- Avoid duplicating MySQL features (e.g., no additional port exposures or protocol-specific props).
- Never use fixed ports or static credentials; rely on Testcontainers for ephemeral bindings and generated values.
- Steer clear of reimplementing core JDBC logic; extend abstract provider instead.

## UNIQUE STYLES
- Minimalist design: Single provider class with straightforward container creation.
- Testing: Spock specs verify basic startup, connectivity, and property customizations.
- Differs from MySQL: No X Protocol support or extra property resolutions.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc-mariadb:build   # Build the module
./gradlew :micronaut-test-resources-jdbc-mariadb:check   # Run integration tests
./gradlew publishToMavenLocal                  # Publish for local use
```

## NOTES
- Ensures compatibility with MariaDB specifics while maintaining JDBC aggregator consistency.
- For MySQL similarities, see test-resources-jdbc-mysql docs; this module avoids redundancy.
- Total lines: ~50 (focused and concise).
