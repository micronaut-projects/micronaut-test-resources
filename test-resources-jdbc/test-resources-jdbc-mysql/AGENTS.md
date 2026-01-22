# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources JDBC MySQL: Testcontainers-based provider for automatic MySQL database resources in Micronaut tests. Launches MySQL containers on demand for JDBC datasources.

## STRUCTURE
```
./test-resources-jdbc-mysql/
├── src/main/java/io/micronaut/testresources/mysql/   # Provider implementation
│   └── MySQLTestResourceProvider.java
├── src/main/resources/META-INF/services/            # Service loader config
├── src/test/groovy/io/micronaut/testresources/jdbc/mysql/  # Spock integration tests
│   ├── StartMySQLTest.groovy
│   └── MySQLBookRepository.groovy
├── src/test/resources/                              # Test config and logs
│   ├── application-test.yml
│   └── logback.xml
└── build.gradle                                     # Module build script
```
Single-module structure focused on MySQL-specific Testcontainers integration.

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/mysql/MySQLTestResourceProvider.java
- Base JDBC abstractions → ../test-resources-jdbc-core/src/main/java/io/micronaut/testresources/jdbc/AbstractJdbcTestResourceProvider.java (extended by this module)
- Integration tests → src/test/groovy/io/micronaut/testresources/jdbc/mysql/StartMySQLTest.groovy
- Dependencies → build.gradle (Testcontainers MySQL, MySQL Connector Java)
- Service registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CODE MAP
Java/Groovy codebase extending shared JDBC core for MySQL specifics. Key class: MySQLTestResourceProvider handles container creation, port exposure (3306 + 33060 for X protocol), and property resolution including x-protocol-url.

## CONVENTIONS
- MySQL image tag: Defaults to official Oracle MySQL Community Server (e.g., container-registry.oracle.com/mysql/community-server:8.0); overridable via 'containers.mysql.image-name'
- JDBC URL props: Resolves 'url', 'username', 'password', 'driver-class-name' under 'datasources.{name}.*'; additionally provides 'x-protocol-url' for MySQL X DevAPI (e.g., mysqlx://user:pass@host:33060/db)
- Property namespace: 'containers.mysql.*' for customizations like image, init scripts; follows core JDBC conventions for consistency
- Container config: Uses ephemeral ports; exposes MySQL port (3306) and X protocol port (33060) dynamically
- Testing: Spock specs verify container startup, connectivity, and custom property resolution

## ANTI-PATTERNS (THIS PROJECT)
- Hardcoded port 3306: Avoid assuming fixed ports; always use container.getMappedPort(MySQLContainer.MYSQL_PORT) for dynamic binding
- Direct image assumptions: Do not hardcode Docker Hub paths; provider handles compatibility for official Oracle images
- Duplicate resolution logic: Extend AbstractJdbcTestResourceProvider instead of reimplementing JDBC URL construction

## UNIQUE STYLES
- X Protocol support: Unique to MySQL provider; resolves additional 'x-protocol-url' property not present in other DB providers
- Image compatibility: Special handling for Oracle's official registry vs. Testcontainers' default Docker Hub

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc-mysql:build   # Build module
./gradlew :micronaut-test-resources-jdbc-mysql:check   # Run tests
./gradlew publishToMavenLocal                # Publish locally (from root)
```

## NOTES
- Differences from MariaDB: MySQL uses official Oracle image with compatibility shims and supports X Protocol (extra port/property); MariaDB uses simpler 'mariadb' image without X Protocol. MySQL provider is more complex due to image handling and extra features. Both extend same abstract class but MySQL adds custom port exposure and resolution.
- Integrates with Micronaut Data JDBC for repository testing in specs.
- Ensure Testcontainers dependencies are aligned for compatibility.
