# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Oracle Test Pilot Provider: Experimental integration with Oracle Test Pilot for Third-Party Software, providing JDBC database resources via environment variables without local containers.

## STRUCTURE
```
./test-resources-jdbc-oracle-test-pilot/
├── src/main/java/io/micronaut/testresources/oracle/testpilot/OracleTestPilotTestResourceProvider.java  # Core provider class
├── src/test/java/io/micronaut/testresources/oracle/testpilot/TestOracleTestPilotTestResourceProvider.java  # Unit tests for provider
├── src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver  # Service registration
├── src/test/resources/logback.xml  # Test logging configuration
├── build.gradle  # Module build script with dependencies (e.g., ojdbc8)
└── (IDE files: .project, .classpath, etc.)
```
Single-module structure focused on Oracle Test Pilot-specific resolution logic.

## WHERE TO LOOK
- Resolution logic → src/main/java/.../OracleTestPilotTestResourceProvider.java (implements ToggableTestResourcesResolver)
- Environment variables used: TESTPILOT_CONNECTION_STRING_SUFFIX, TESTPILOT_USERNAME, TESTPILOT_PASSWORD
- Test verification → src/test/java/... (ensures property resolution from env vars)
- Dependencies → build.gradle (integrates with micronaut-test, ojdbc8)

## CODE MAP
Compact Java module: One main class for property resolution, unit tests, and service loader. No complex architecture.

## CONVENTIONS
- Property namespace: \"datasources.{name}.*\" (url, username, password, driver-class-name, dialect, schema-generate)
- Resolution: Pulls values from specific environment variables; hard-codes driver and dialect to Oracle specifics
- Extension: Implements core TestResourcesResolver interface for seamless integration
- Testing: JUnit5-based unit tests verifying resolution logic

## ANTI-PATTERNS (THIS MODULE)
- Credential handling: Relies on environment variables; avoid exposing sensitive info (e.g., passwords) in unsecured envs
- No container usage: Differs from other JDBC providers; not suitable for fully local/offline testing
- Hard-coded values: Some properties (e.g., driver, dialect) are fixed; consider configurability for flexibility

## UNIQUE STYLES
- Env var-based provisioning instead of Testcontainers for external service integration
- Experimental \"test-pilot\" nature: Designed for Oracle's pilot program, may evolve based on feedback

## COMMANDS
```bash
./gradlew :micronaut-test-resources-jdbc-oracle-test-pilot:build  # Build module
./gradlew :micronaut-test-resources-jdbc-oracle-test-pilot:test   # Run unit tests
./gradlew :micronaut-test-resources-jdbc-oracle-test-pilot:publishToMavenLocal  # Install locally
```

## NOTES
- Experimental: This module is a pilot for Oracle Test Pilot integration; use with caution in production tests
- Deprecation potential: Parent project notes deprecated Oracle XE variants; prefer unified 'oracle' providers for standard use
- Requirements: Needs Oracle Test Pilot setup and env vars; fails without them
- Since 2.9.0: Introduced for cloud-based testing alternatives to local containers
