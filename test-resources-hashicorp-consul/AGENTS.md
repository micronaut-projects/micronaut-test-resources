# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
The test-resources-hashicorp-consul module provides automatic provisioning of a HashiCorp Consul test container for Micronaut applications during testing. It integrates with the Micronaut Test Resources core SPI to supply Consul-related configuration properties like host, port, and default zone. This allows seamless integration testing with Consul without manual setup.

## STRUCTURE
```
./test-resources-hashicorp-consul/
├── build.gradle                          # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/consul/  
│   │   │   └── ConsulTestResourceProvider.java  # Core provider class
│   │   └── resources/META-INF/services/  
│   │       └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
│   └── test/
│       ├── groovy/io/micronaut/testresources/hashicorp/consul/  
│       │   └── ConsulStartedTest.groovy  # Integration tests
│       └── resources/  
│           ├── bootstrap.yml            # Test bootstrap config
│           └── logback.xml              # Logging config for tests
└── .settings/                            # Eclipse/IDE settings (ignore for core logic)
```

## WHERE TO LOOK
- Core resolution logic → src/main/java/io/micronaut/testresources/consul/ConsulTestResourceProvider.java (handles container startup, property resolution)
- Property conventions → Look for constants like PROPERTY_CONSUL_CLIENT_HOST, PROPERTY_CONSUL_CLIENT_PORT
- Test examples → src/test/groovy/io/micronaut/testresources/hashicorp/consul/ConsulStartedTest.groovy (verifies container starts and properties inject correctly)
- Dependencies → build.gradle (testcontainers.consul, micronaut.discovery)
- SPI integration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver

## CODE MAP
Small module with single provider class extending AbstractTestContainersProvider. Key methods:
- getResolvableProperties(): Defines supported properties (consul.client.host/port/default-zone)
- startContainer(): Creates ConsulContainer with configurable image and commands
- resolve(): Maps container host/port to properties; supports KV property injection via containers.hashicorp-consul.kv-properties

## CONVENTIONS
- Properties follow namespace: consul.client.* (e.g., host, port, default-zone)
- Host prop (consul.client.host): Resolved to container's IP address (usually localhost)
- Port prop (consul.client.port): Dynamically assigned random port (e.g., 8500 mapped randomly)
- Default zone: Combines host:port for convenience
- Image: Defaults to 'hashicorp/consul'; override via test-resources.containers.hashicorp-consul.image-name
- KV props: Inject key-values via containers.hashicorp-consul.kv-properties map

## ANTI-PATTERNS (THIS PROJECT)
- Fixed port mapping: Do NOT hard-code ports (e.g., avoid .withExposedPorts(8500)); we intentionally do NOT set port bindings to prefer ephemeral/random assignments and avoid conflicts.
- Avoid blocking ops in resolvers; keep lightweight as it runs in test context.
- Don't duplicate core Testcontainers logic; extend AbstractTestContainersProvider.

## UNIQUE STYLES
- Groovy-based tests with Spock for concise verification.
- Uses Micronaut's @Value injection for properties in tests.
- Integrates with micronaut-discovery-consul for client testing.

## COMMANDS
```bash
cd test-resources-hashicorp-consul
../gradlew build     # Build and test this module
../gradlew check     # Run tests
../gradlew publishToMavenLocal  # Local install
```

## NOTES
- Module depends on Testcontainers Consul module for container management.
- Supports custom Consul commands via test-resources.containers.hashicorp-consul.commands.
- For production-like testing, configure KV properties to mimic real Consul setups.
- Part of larger micronaut-test-resources suite; see root AGENTS.md for ecosystem context.
