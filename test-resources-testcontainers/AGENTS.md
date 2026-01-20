# MODULE KNOWLEDGE BASE: test-resources-testcontainers

Generated: 2026-01-20
Commit: 8d7a43fdd0becc33eeaa1912b28b850da5aca59c
Branch: kafka-native

## OVERVIEW
Base abstractions for container-backed test resources providers using Testcontainers.

## STRUCTURE
```
./
├── src/main/java/io/micronaut/testresources/testcontainers/  # Core provider classes and utilities
├── src/test/groovy/io/micronaut/testresources/testcontainers/  # Spock tests for providers
└── build.gradle.kts  # Module build configuration
```

## WHERE TO LOOK
- AbstractTestContainersProvider: Base class for implementing specific container providers, handles creation, configuration, and property resolution.
- DockerSupport: Utility class for checking Docker availability with timeout and caching to avoid blocking calls.
- TestContainerMetadataSupport: Handles application of configuration metadata to containers, including wait strategies, networks, binds, etc.
- GenericTestContainerProvider: Extends abstract provider for generic containers not tied to specific services.

## CODE MAP
- Main package: io.micronaut.testresources.testcontainers
- Key classes: AbstractTestContainersProvider (abstract), DockerSupport (final), TestContainerMetadataSupport (final), TestContainers (manages container lifecycle).
- Tests: Comprehensive Spock specs in src/test/groovy covering provider resolution, container management, and metadata application.

## CONVENTIONS
- Image naming: Define default in getDefaultImageName(); overridable via test-resources.containers.{name}.image-name and .image-tag.
- Wait strategies: Configurable per container via test-resources.containers.{name}.wait-strategy.{type} (log, http, port, healthcheck, all) with sub-properties like regex, path, timeout.
- Network usage: Set shared networks with .network, aliases with .network-aliases, mode with .network-mode for inter-container communication.
- 'do NOT set fixed ports' for certain providers: Always use ephemeral/random port bindings; map via exposed-ports config to get mapped ports.

## ANTI-PATTERNS (THIS MODULE)
- Hard-coded ports: Never fix ports in providers; rely on Testcontainers' dynamic mapping to avoid conflicts.
- Global state: Avoid static shared state like DockerSupport's AtomicReference for availability; can lead to issues in concurrent environments.

## UNIQUE STYLES
- Serves as base for technology-specific modules (e.g., JDBC, Kafka) which extend AbstractTestContainersProvider.
- Supports generic containers with YAML config for arbitrary services not covered by built-in providers.
- Integrates with core test resources SPI via ToggableTestResourcesResolver.

## COMMANDS
```bash
../gradlew :micronaut-test-resources-testcontainers:build  # Build this module
../gradlew :micronaut-test-resources-testcontainers:test   # Run tests
```

## NOTES
- Requires Docker; checks availability non-blockingly with configurable timeout.
- Many downstream modules extend these bases for specific resources like databases, messaging.
- Configuration-driven: Extensive options for env, labels, binds, tmpfs, copies, memory limits via test-resources config.
- Avoids parent project duplication by focusing on Testcontainers-specific abstractions.
