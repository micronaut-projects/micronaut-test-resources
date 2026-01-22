# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: unknown (module-specific)
Branch: main

## OVERVIEW
test-resources-opensearch: Micronaut Test Resources module for provisioning OpenSearch containers via Testcontainers. Automatically provides connection details for Micronaut's OpenSearch clients during tests.

## STRUCTURE
```
./test-resources-opensearch/
├── build.gradle.kts              # Module build configuration
├── src/main/java/.../OpenSearchTestResourceProvider.java  # Core provider implementation
├── src/main/resources/META-INF/services/...  # SPI registration
├── src/test/groovy/...           # Spock tests (AbstractOpenSearchSpec, CustomOpenSearchImageSpec, OpenSearchStartedSpec)
└── src/test/resources/logback.xml  # Test logging config
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/opensearch/OpenSearchTestResourceProvider.java
- Test examples → src/test/groovy/io/micronaut/testresources/opensearch/*Spec.groovy
- Dependencies/config → build.gradle.kts (depends on test-resources-testcontainers, opensearch-testcontainers)
- Service registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CODE MAP
- OpenSearchTestResourceProvider: Extends AbstractTestContainersProvider; creates OpenSearchContainer; resolves http-hosts properties.
- Tests: Demonstrate auto-start, custom image config, client injection via @Value.

## CONVENTIONS
- Cluster name: Defaults to OpenSearchContainer's built-in (typically 'opensearch-cluster'); not customizable in provider.
- Image tag: Not explicitly set; uses latest from 'opensearchproject/opensearch' unless overridden via 'test-resources.containers.opensearch.image-name' property.
- JVM opts: No direct support in provider; configure via container environment variables if needed (e.g., in custom images or extended providers).

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code ports; provider uses ephemeral/random bindings via Testcontainers.
- Avoid legacy Elasticsearch APIs/clients; use Micronaut's OpenSearch-specific modules (e.g., micronaut-opensearch-rest-client).
- Do NOT modify container startup without extending the provider; respect Test Resources SPI for resolvability.

## UNIQUE STYLES
- Follows standard Test Resources pattern: property resolution for 'micronaut.opensearch.[rest-client|httpclient5].http-hosts'.
- Tests use Spock with @MicronautTest and TestContainers.closeAll() for cleanup.

## COMMANDS
```bash
../gradlew :micronaut-test-resources-opensearch:build  # Build module
../gradlew :micronaut-test-resources-opensearch:check  # Run tests
../gradlew :micronaut-test-resources-opensearch:publishToMavenLocal
```

## NOTES
- Integrates with org.opensearch.testcontainers:opensearch for container management.
- Supports custom images for version pinning or plugins (see CustomOpenSearchImageSpec).
- No AOT or custom source sets; pure test resource provider module.
- Ensure Testcontainers is enabled in build for usage.
- For production, use real OpenSearch clusters, not test containers.
