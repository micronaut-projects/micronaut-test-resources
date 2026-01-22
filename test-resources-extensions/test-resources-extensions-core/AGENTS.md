# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
The test-resources-extensions-core module provides the base infrastructure for integrating Micronaut Test Resources with testing frameworks. It defines the core SPI and annotations for declarative injection of test properties, enabling tests to request resources like database connections without manual setup. This module is framework-agnostic, focusing on property resolution mechanics, while specific test framework integrations (e.g., JUnit) are handled in separate modules.

## STRUCTURE
```
test-resources-extensions-core/
├── src/main/java/io/micronaut/test/extensions/testresources/
│   ├── TestResourcesPropertiesFactory.java
│   ├── TestResourcesPropertyProvider.java
│   ├── annotation/TestResourcesProperties.java
│   ├── TestResourcesClientHolder.java
├── src/main/resources/META-INF/services/
│   └── io.micronaut.test.support.TestPropertyProviderFactory
```

## WHERE TO LOOK
- Core extension wiring: TestResourcesPropertiesFactory.java for how annotations are processed into property providers.
- Annotation definitions: annotation/TestResourcesProperties.java
- Property provider SPI: TestResourcesPropertyProvider.java for custom derivations.
- Client injection: TestResourcesClientHolder.java for testing utilities.

## CODE MAP
- TestResourcesPropertiesFactory: Implements TestPropertyProviderFactory to scan for @TestResourcesProperties and create providers.
- TestResourcesPropertyProvider: Functional interface for providers that compute additional properties from resolved ones.
- @TestResourcesProperties: Annotation to specify required properties and custom providers on test classes.
- TestResourcesClientHolder: Utility for injecting mock clients in unit tests.
- Service loader: Registers the factory via META-INF/services.

## CONVENTIONS
- Annotations: Use @TestResourcesProperties on test classes to declare required properties (e.g., value = {\"jdbc.url\"}) and optional providers.
- Lifecycle hooks: Providers are invoked after initial resolution but before context startup, allowing derivation of dependent properties. Implement TestResourcesPropertyProvider for custom logic.
- Naming: Properties follow Micronaut conventions, resolved via Test Resources resolvers.
- Integration: Designed to be extended by framework-specific modules for test lifecycle management.

## ANTI-PATTERNS (THIS MODULE)
- Mixing test frameworks: Do not attempt to use core annotations without a framework-specific extension; lifecycle integration requires adapters like junit-platform.
- Overriding core SPI: Avoid direct implementation of internal interfaces; use provided extension points.
- Hardcoding resolutions: Rely on annotation-driven config instead of manual property setting in tests.

## UNIQUE STYLES
- Service loader based registration for factories.
- Annotation processors for property injection.
- Minimal dependencies to keep it framework-agnostic.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-extensions-core:build  # Build this module
./gradlew :micronaut-test-resources-extensions-core:test    # Run tests
```

## NOTES
- This module does not include test execution logic; see test-resources-extensions-junit-platform for JUnit/Spock/Kotest integration.
- Custom providers should be stateless and thread-safe.
- For advanced usage, combine with test-resources-client for client-side resolution.
