# test-resources-core KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fdd0becc33eeaa1912b28b850da5aca59c
Branch: kafka-native

## OVERVIEW
Core module providing the SPI, model, and utilities for resolving test resources in Micronaut applications.

## STRUCTURE
```
./test-resources-core/
├── src/main/java/io/micronaut/testresources/core/  # Core classes and interfaces
├── build.gradle                                   # Module build configuration
└── ...                                            # Build artifacts
```

## WHERE TO LOOK
- Core resolution SPI → src/main/java/io/micronaut/testresources/core/TestResourcesResolver.java
- Scope management → src/main/java/io/micronaut/testresources/core/Scope.java
- Resolver loading → src/main/java/io/micronaut/testresources/core/ResolverLoader.java
- Property source integration → src/main/java/io/micronaut/testresources/core/LazyTestResourcesPropertySourceLoader.java
- Expression resolution → src/main/java/io/micronaut/testresources/core/LazyTestResourcesExpressionResolver.java
- Utility support → src/main/java/io/micronaut/testresources/core/PropertyResolverSupport.java
- Toggable resolvers → src/main/java/io/micronaut/testresources/core/ToggableTestResourcesResolver.java
- Property expression producer → src/main/java/io/micronaut/testresources/core/PropertyExpressionProducer.java

## CODE MAP
Key classes and interfaces:
- **TestResourcesResolver**: Defines the contract for resolvers; key methods: resolve(), getResolvableProperties(), getRequiredProperties().
- **Scope**: Manages hierarchical scopes for resource lifecycles; supports child scopes and inclusion checks.
- **ResolverLoader**: Interface for loading and retrieving resolver instances.
- **ToggableTestResourcesResolver**: Extends TestResourcesResolver with enable/disable logic based on configuration.
- **PropertyExpressionProducer**: Produces lists of resolvable property keys from configuration.
- **PropertyResolverSupport**: Helper for resolving required properties and checking resolvability.
- **LazyTestResourcesPropertySourceLoader**: Integrates lazy resolution into Micronaut's property sources.
- **LazyTestResourcesExpressionResolver**: Handles resolution of lazy placeholders.

## CONVENTIONS
- **Resolver SPI**: Implement TestResourcesResolver and register via META-INF/services/io.micronaut.testresources.core.TestResourcesResolver for automatic discovery and loading.
- **Property namespaces**: Use \"test-resources\" for global config; resolver-specific like \"[resolver-name].enabled\" for toggling.
- **Implicit scope**: Always factored into resolution; configured via \"micronaut.test.resources.scope\"; defaults to ROOT if unspecified.
- Resolvers implement Ordered for precedence.
- Return Optional.empty() for unsupported properties to allow chaining.

## ANTI-PATTERNS (THIS MODULE)
- Do not perform I/O in resolvers; keep resolution logic pure and delegate heavy operations to extensions.
- Avoid tight coupling to specific providers; design for extensibility and abstraction.
- Do not ignore scopes, as this can lead to resource leaks across test executions.
- Avoid hardcoding property values; leverage getRequiredProperties() for dynamic dependencies.
- Do not throw exceptions in resolve() for unsupported cases; use Optional.empty().

## UNIQUE STYLES
- Extensive use of Java Optional for handling resolution outcomes.
- Tight integration with Micronaut's property expression and source mechanisms.
- Emphasis on lazy evaluation to avoid unnecessary resolutions.

## COMMANDS
```bash
../gradlew :micronaut-test-resources-core:build  # Build this module
../gradlew :micronaut-test-resources-core:check  # Run tests
```

## NOTES
- Foundational for other modules like test-resources-testcontainers and database-specific ones.
- No executable main; purely SPI and utilities for integration.
- Focus on resolution mechanics; actual resource provisioning (e.g., containers) handled in dependent modules.
- High reuse: Underpins all test resource resolution across the repo.
