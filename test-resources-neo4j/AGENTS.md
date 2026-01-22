# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:23:25Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Neo4j module: Provides automatic provisioning of Neo4j graph databases for tests using Testcontainers. Resolves 'neo4j.uri' property to a bolt URL without authentication by default.

## STRUCTURE
```
./test-resources-neo4j/
├── build.gradle                          # Module build configuration
├── src/main/java/io/micronaut/testresources/neo4j/  # Provider implementation
│   └── Neo4jTestResourceProvider.java
├── src/main/resources/META-INF/services/ # Service loader for resolver
│   └── io.micronaut.testresources.core.TestResourcesResolver
├── src/test/groovy/io/micronaut/testresources/neo4j/  # Spock test specs
│   ├── AbstractNeo4jDBSpec.groovy
│   ├── Book.groovy
│   ├── BookRepository.groovy
│   └── Neo4jStartedTest.groovy
└── src/test/resources/                   # Test config (logback.xml)
```

## WHERE TO LOOK
- Main provider class → src/main/java/io/micronaut/testresources/neo4j/Neo4jTestResourceProvider.java
- Test specs → src/test/groovy/io/micronaut/testresources/neo4j/* (Spock tests verifying container startup and basic ops)
- Build config → build.gradle (dependencies: testcontainers-neo4j, micronaut-neo4j-bolt)
- Service registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CODE MAP
Small single-purpose module:
- Neo4jTestResourceProvider extends AbstractTestContainersProvider<Neo4jContainer<?>>
  - getResolvableProperties(): Returns [neo4j.uri]
  - createContainer(): Instantiates Neo4jContainer with dynamic ports, no auth
  - resolveProperty(): Maps neo4j.uri to container.getBoltUrl()
- Tests: AbstractNeo4jDBSpec (base) + Neo4jStartedTest (verifies CRUD via BookRepository)

Skipped detailed LSP map due to module simplicity.

## CONVENTIONS
- Bolt URL property: 'neo4j.uri' resolved to container's bolt URL (e.g., bolt://localhost:<random-port>)
- Auth defaults: Authentication disabled via container.withoutAuthentication() (no username/password)
- Image tag: Defaults to 'neo4j'; configurable via testResourcesConfig
- No fixed ports: Uses Testcontainers ephemeral port bindings for bolt (7687 internally, random external)
- Property namespace: 'neo4j' (simple name)
- Follows core Test Resources SPI for resolver registration

## ANTI-PATTERNS (THIS MODULE)
- Do NOT rely on debug ports or fixed port mappings; always use dynamic ports
- Avoid hard-coding container configurations; prefer testResourcesConfig overrides
- Note: Copy-paste errors in comments/build.gradle (mentions 'MongoDB' instead of 'Neo4j') – fix in updates
- Do NOT introduce blocking operations; align with Micronaut's non-blocking nature

## UNIQUE STYLES
- Testing: Exclusively Spock (Groovy) with abstract spec for shared setup
- Minimalist: No aggregators or submodules; direct extension of test-resources-testcontainers abstractions
- Integration: Uses Micronaut Neo4j Bolt for test repository (BookRepository)

## COMMANDS
```bash
./gradlew :micronaut-test-resources-neo4j:build    # Build module
./gradlew :micronaut-test-resources-neo4j:test     # Run tests
./gradlew :micronaut-test-resources-neo4j:publishToMavenLocal
# Full project build: ./gradlew build (from root)
```

## NOTES
- Dependencies: Relies on org.testcontainers:neo4j for container mgmt
- Scope: Test-only; integrates with micronaut-neo4j-bolt for driver
- Issues: Description in build.gradle incorrectly states 'MongoDB' (copy-paste artifact)
- Best practice: Extend for custom Neo4j configs (e.g., with auth) via test resources config
- Aligned with project-wide anti-patterns: No hardcoded ports, no reflection
