# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Embedded: Module for in-process loading and resolution of test resources resolvers via service loading, enabling direct integration without a remote server.

## STRUCTURE
```
test-resources-embedded/
├── build.gradle                             # Module build configuration with custom test suite 'test2'
├── src/main/java/io/micronaut/testresources/embedded/
│   ├── TestResourcesResolverLoader.java     # Loads and caches TestResourcesResolver instances
│   ├── EmbeddedTestResourcesPropertySourceLoader.java  # Produces property sources from resolvers with kebab-case validation
│   └── EmbeddedTestResourcesPropertyExpressionResolver.java  # Resolves property expressions for embedded resources
├── src/main/resources/META-INF/services/    # Service loader registrations for property source and expression resolvers
├── src/test/groovy/                         # Standard unit tests (e.g., FakeKafka resolver tests)
├── src/test2/groovy/                        # Custom source set for testing invalid or failing resolvers
└── (Note: Documentation referenced from project-level src/main/docs/)
```

## WHERE TO LOOK
- Embedded resolvers loading → TestResourcesResolverLoader.java (uses SoftServiceLoader for discovery and caching)
- Property source and expression resolution → EmbeddedTestResourcesPropertySourceLoader.java and EmbeddedTestResourcesPropertyExpressionResolver.java
- Custom test isolation for edge cases → src/test2/groovy/ (e.g., InvalidTestResourcesTest.groovy, FailingResolver.java)
- Service registrations → src/main/resources/META-INF/services/
- Architecture overview → Project docs: guide/architecture-embedded.adoc (explains in-process resolution)

## CODE MAP
(Skipped as LSP not initialized. This is a small Java/Groovy module focused on resolver aggregation and property handling.)

## CONVENTIONS
- Resolvers are discovered and loaded via Java ServiceLoader mechanism, with deduplication by class and ordering.
- Property keys enforced in kebab-case format; validation occurs at resolution time to prevent errors.
- Custom source set 'src/test2' used for isolated testing of invalid resolvers, preventing interference with main tests.
- Emphasizes in-process resolution: No reliance on remote server or client; all operations are local to the application context.

## ANTI-PATTERNS (THIS PROJECT)
- Relying on container-only features: Embedded mode supports resolvers that may use containers internally but should not assume container availability; design for pure in-process where possible.
- Direct embedding in applications: Avoid to prevent classpath pollution from resolver dependencies; prefer remote server for production-like testing.
- Using camel-case or invalid property keys: Strictly enforced to throw exceptions, ensuring consistency.
- Hard-coding ports or resources: Use ephemeral/random bindings in resolvers.

## UNIQUE STYLES
- Custom test suite in build.gradle for 'test2' source set, allowing separate compilation and execution for failure scenarios.
- Aggregation of all resolvers via service loading without defining new providers in this module.
- Delegation to core SPI for resolution, focusing on embedding mechanics.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-embedded:build    # Build the module
./gradlew :micronaut-test-resources-embedded:test     # Run standard tests
./gradlew :micronaut-test-resources-embedded:test2    # Run custom test suite for invalid cases
```

## NOTES
- Designed for in-process resolution without remote server, integrating directly with Micronaut's property source system.
- Includes test fixtures like failing resolvers to verify error handling.
- Low complexity; acts as a bridge for embedding core resolvers into applications or servers.
- Warning: Direct use in apps is discouraged; optimal for internal use in test-resources-server.
