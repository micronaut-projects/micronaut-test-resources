# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:24:51Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Redis: Module providing automatic test resources for Redis, supporting single-node and cluster modes via Testcontainers integration.

## STRUCTURE
```
./test-resources-redis/
├── build.gradle                  # Module build configuration
├── src/main/java/io/micronaut/testresources/redis/
│   ├── RedisTestResourceProvider.java        # Single instance provider
│   ├── RedisClusterTestResourceProvider.java # Cluster mode provider
│   └── RedisConfigurationSupport.java        # Configuration utilities
├── src/main/resources/META-INF/services/     # SPI registration
├── src/test/groovy/io/micronaut/testresources/redis/
│   ├── AbstractRedisSpec.groovy
│   ├── RedisAccess.groovy
│   ├── RedisClusterAccess.groovy
│   ├── RedisStartedTest.groovy              # Single mode tests
│   └── RedisClusterStartedTest.groovy        # Cluster mode tests
└── src/test/resources/                       # Test configs (e.g., application-cluster.yml)
```

## WHERE TO LOOK
- Core provider logic → src/main/java/io/micronaut/testresources/redis/*Provider.java
- Configuration keys → RedisConfigurationSupport.java (containers.redis.* namespace)
- Property resolution → redis.uri (single), redis.uris (cluster)
- Tests → src/test/groovy/*Test.groovy for startup and access verification

## CODE MAP
Small module extending abstract Testcontainers providers. Key classes:
- RedisTestResourceProvider: Resolves single Redis URI
- RedisClusterTestResourceProvider: Resolves cluster URIs with multi-node config
- Registered via META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CONVENTIONS
- Modes: Single (default, redis.uri) vs Cluster (enabled via containers.redis.cluster-mode: true, resolves redis.uris)
- Image selection: Uses Testcontainers defaults (redis:latest for single, configurable)
- Timeouts: Configurable via cluster settings; no blocking waits in resolvers
- Ports: Ephemeral/random, no fixed ports; configurable initial port for clusters
- Namespaces: Properties under redis.*, configs under containers.redis.*
- Extends test-resources-testcontainers base abstractions

## ANTI-PATTERNS (THIS MODULE)
- Avoid blocking waits in resolution logic
- No hard-coded passwords or credentials; use container defaults
- Do not hard-code ports; rely on dynamic bindings
- Preserve SPI conventions; do not add custom resolvers outside META-INF

## UNIQUE STYLES
- Simple, non-aggregator module focused on Redis
- Heavy use of Spock for tests with shared abstract specs
- Cluster mode uses custom config template for Redis settings

## COMMANDS
```bash
./gradlew :micronaut-test-resources-redis:build     # Build module
./gradlew :micronaut-test-resources-redis:check     # Run tests
./gradlew :micronaut-test-resources-redis:publishToMavenLocal
```

## NOTES
- Depends on test-resources-core, test-resources-testcontainers, and com.redis:testcontainers-redis
- Cluster mode configurable: masters (default 3), slaves-per-master (default 1), notify-keyspace-events
- Tests verify container startup and basic Redis operations without fixed ports
- Integrates with Micronaut's test resources SPI for automatic provisioning
