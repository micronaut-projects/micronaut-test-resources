# PROJECT KNOWLEDGE BASE

Generated: 2026-01-22T12:14:22Z
Commit: da5801d0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Kafka: Spawns a Kafka testcontainer and resolves kafka.bootstrap.servers.

## STRUCTURE
```
./test-resources-kafka/
├── build.gradle
├── src/main/java/io/micronaut/testresources/kafka/
│   └── KafkaTestResourceProvider.java      # Provider (extends AbstractTestContainersProvider)
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
├── src/test/groovy/io/micronaut/testresources/kafka/
│   ├── AbstractKafkaSpec.groovy
│   ├── KafkaStartedTest.groovy
│   ├── KafkaNotStartedTest.groovy
│   ├── KafkaCustomConfigTest.groovy
│   └── CustomKafkaImageTest.groovy
└── src/test/resources/
    ├── application-custom.yml
    └── example/kafka_server_jaas.conf
```

## WHERE TO LOOK
- Provider logic → KafkaTestResourceProvider.java (DEFAULT_IMAGE: apache/kafka:4.1.1; DISPLAY_NAME: "Kafka")
- Resolved property → kafka.bootstrap.servers (container.getBootstrapServers())
- SPI hook → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver
- Tests → src/test/groovy/* for startup, custom image, and failure cases

## CODE MAP
- Simple provider: shouldAnswer only for kafka.bootstrap.servers
- Image selection: default image overridable via property
- No custom waits; relies on Testcontainers' KafkaContainer defaults

## CONVENTIONS
- Namespaces: kafka.* for resolved props; containers.kafka.* for configuration
- Ports: always ephemeral/random; do not map fixed ports
- Image override supported: test-resources.containers.kafka.image-name (see CustomKafkaImageTest)
- No Zookeeper required; modern KafkaContainer

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code ports or endpoints; always use container.getBootstrapServers()
- Do NOT embed JAAS/user credentials here; tests may supply configs
- Avoid blocking operations in providers

## COMMANDS
```bash
./gradlew :micronaut-test-resources-kafka:build
./gradlew :micronaut-test-resources-kafka:check
./gradlew :micronaut-test-resources-kafka:publishToMavenLocal
```

## NOTES
- Depends on test-resources-testcontainers and core SPI
- Build uses io.micronaut.build.internal.testcontainers-module and kafka-testing conventions
