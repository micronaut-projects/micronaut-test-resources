# MODULE KNOWLEDGE BASE (test-resources-solr)

Generated: 2026-01-20T13:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources module for Apache Solr: Provides automatic provisioning of Solr search server via Testcontainers for testing.

## STRUCTURE
```
test-resources-solr/
├── build.gradle.kts                     # Module build configuration
├── src/main/java/.../solr/              # Provider implementation
├── src/main/resources/META-INF/         # SPI registration
├── src/test/groovy/.../solr/            # Spock tests for provider
└── .settings/ .project .classpath       # Eclipse/IDE configs
```

## WHERE TO LOOK
- Core provider → src/main/java/io/micronaut/testresources/solr/SolrTestResourceProvider.java
- Testcontainers integration → Extends AbstractTestContainersProvider&lt;SolrContainer&gt;
- Tests → src/test/groovy/io/micronaut/testresources/solr/*Spec.groovy
- Dependencies → build.gradle.kts (libs.managed.solr.testcontainers)

## CODE MAP
Main class: SolrTestResourceProvider
- Creates SolrContainer with image &quot;solr:9.8.0&quot;
- Configures Zookeeper, collections, configs, schemas
- Resolves properties like micronaut.solr.hosts

## CONVENTIONS
- Properties under solr.* namespace: solr.collection, solr.config.name/url, solr.schema.url, solr.zookeeper.enabled
- Exposed: micronaut.solr.hosts (http://host:port/solr), micronaut.solr.zk-hosts
- Uses dynamic ports via container.getSolrPort(), getZookeeperPort()
- Default: Zookeeper enabled, ephemeral ports
- Cores/collections configured via solr.collection

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code ports; always use dynamic mapping to avoid conflicts (uses ephemeral/random bindings)
- Avoid mixing with Elasticsearch/OpenSearch configs or properties (distinct solr.* namespace)
- Do NOT assume fixed image; configurable via test resources config
- Preserve namespace separation: solr.* for config, micronaut.solr.* for resolved endpoints

## UNIQUE STYLES
- Heavy use of Spock for testing container startup and property resolution
- Custom container configuration overriding SolrContainer's configure()
- Handles optional config/schema URLs with validation

## COMMANDS
```bash
./gradlew :micronaut-test-resources-solr:build     # Build module
./gradlew :micronaut-test-resources-solr:test      # Run tests
./gradlew :micronaut-test-resources-solr:publishToMavenLocal
```

## NOTES
- Javadoc in provider class incorrectly mentions &quot;OpenSearch&quot; - update to &quot;Solr&quot;
- Module is standalone but integrates with core test-resources SPI
- Tests verify container image, property injection, ZK hosts
"
