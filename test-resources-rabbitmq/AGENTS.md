# MODULE KNOWLEDGE BASE: test-resources-rabbitmq

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
This module provides a Testcontainers-based test resource provider for RabbitMQ in Micronaut Test Resources.
It automatically provisions a RabbitMQ container for tests requiring AMQP connections, resolving properties like URI, username, and password.

## STRUCTURE
```
./test-resources-rabbitmq/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/rabbitmq/RabbitMQTestResourceProvider.java  # Core provider class
│   │   └── resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver  # Service registration
│   └── test/
│       └── groovy/io/micronaut/testresources/rabbitmq/  # Spock tests: AbstractRabbitMQSpec.groovy, RabbitMQStartedTest.groovy, RabbitMQNotStartedTest.groovy
└── .settings/ .project .classpath        # IDE configuration files
```

## WHERE TO LOOK
- Resolver implementation → src/main/java/.../RabbitMQTestResourceProvider.java (extends AbstractTestContainersProvider)
- Property resolution logic → resolve() method handling rabbitmq.uri, .username, .password
- Container creation → createContainer() using RabbitMQContainer
- Tests for startup behavior → src/test/groovy/.../RabbitMQStartedTest.groovy and RabbitMQNotStartedTest.groovy

## CODE MAP
Minimal codebase focused on a single resolver class. Key methods:
- getResolvableProperties(): Returns supported props (rabbitmq.uri etc.)
- resolve(): Switches on property name to extract from container (e.g., getAmqpUrl(), getAdminUsername())
- createContainer(): Instantiates RabbitMQContainer with configurable image

No complex architecture; leverages test-resources-testcontainers base classes.

## CONVENTIONS
- AMQP URI props: Resolves "rabbitmq.uri" to dynamic "amqp://host:port" using container.getAmqpUrl() with ephemeral ports
- Image tag: Defaults to "rabbitmq" (implying latest); override via test-resources config (e.g., micronaut.test.resources.rabbitmq.image-name)
- vhost/user/password:
  - vhost: Uses default '/' (no explicit resolver property)
  - user: Resolves "rabbitmq.username" from container.getAdminUsername() (default 'guest')
  - password: Resolves "rabbitmq.password" from container.getAdminPassword() (default 'guest')
- Properties follow "rabbitmq." namespace prefix
- Adheres to core test-resources SPI for lazy container startup

## ANTI-PATTERNS (THIS PROJECT)
- Fixed ports: Never hardcode ports; rely on Testcontainers' random ephemeral port assignment to avoid conflicts
- Avoid custom container config unless necessary; stick to defaults for reproducibility
- Do not expose unnecessary properties (e.g., no vhost resolver since defaults suffice)

## UNIQUE STYLES
- Extends AbstractTestContainersProvider<RabbitMQContainer> for boilerplate handling
- Uses immutable sets for resolvable properties
- Spock-based tests with shared AbstractRabbitMQSpec for common setup

## COMMANDS
```bash
./gradlew :micronaut-test-resources-rabbitmq:build    # Build and test module
./gradlew :micronaut-test-resources-rabbitmq:check    # Run tests
./gradlew publishToMavenLocal               # Publish locally (from root)
```

## NOTES
- Depends on testcontainers-rabbitmq and micronaut-rabbitmq
- Container only starts if rabbitmq.* properties are requested in tests
- Integrates via META-INF/services for auto-discovery in test-resources ecosystem
- Small footprint: ~100 LOC in provider; focuses on essential resolution
