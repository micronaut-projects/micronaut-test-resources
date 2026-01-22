# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:23:17Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
MQTT via HiveMQ testcontainer provider.

## STRUCTURE
```
./test-resources-hivemq/
├── build.gradle                               # Module build configuration
├── src/main/java/io/micronaut/testresources/hivemq/  # Core provider implementation
│   └── HiveMQTestResourceProvider.java
├── src/main/resources/META-INF/services/      # SPI service registration
│   └── io.micronaut.testresources.core.TestResourcesResolver
├── src/test/groovy/io/micronaut/testresources/hivemq/  # Spock integration tests
│   ├── AbstractHiveMQSpec.groovy
│   ├── HiveMQNotStartedTest.groovy
│   └── HiveMQStartedTest.groovy
└── src/test/resources/                        # Test resources (logback.xml)
```

## WHERE TO LOOK
- Main provider: src/main/java/io/micronaut/testresources/hivemq/HiveMQTestResourceProvider.java
- Property keys: mqtt.client.client-id, mqtt.client.server-uri (under mqtt.* namespace)
- Base abstractions: Extends AbstractTestContainersProvider from test-resources-testcontainers
- Tests: src/test/groovy/io/micronaut/testresources/hivemq/

## CODE MAP
Small module providing HiveMQ Testcontainers integration for Micronaut test resources. Java for provider, Groovy/Spock for tests.

## CONVENTIONS
- Image tag override: Configurable via test resources properties, defaults to hivemq/hivemq-ce:2021.3
- Ports: No fixed bindings; uses dynamic ports from Testcontainers (container.getMqttPort())
- Wait strategy: Relies on default HiveMQContainer readiness checks (MQTT port availability)
- Property resolution: Handles mqtt.client.* properties for MQTT client configuration

## ANTI-PATTERNS (THIS PROJECT)
- Do NOT hardcode credentials or ports; use dynamic resolution and generated UUIDs for client IDs
- Avoid custom wait strategies unless necessary; stick to Testcontainers defaults

## UNIQUE STYLES
- Generates unique UUID for mqtt.client.client-id to prevent conflicts
- Tests include actual MQTT publish/subscribe using Micronaut MQTT annotations
- Lightweight extension of base testcontainers provider with minimal overrides

## COMMANDS
```bash
cd .. && ./gradlew :micronaut-test-resources-hivemq:build  # Build the module
cd .. && ./gradlew :micronaut-test-resources-hivemq:test   # Run unit/integration tests
cd .. && ./gradlew :micronaut-test-resources-hivemq:publishToMavenLocal  # Publish to local Maven repo
```

## NOTES
- Context: Extends base testcontainers provider for container lifecycle management.
- Dependencies: testcontainers-hivemq, micronaut-mqttv5 (for tests)
- Integration: Provides server URI and client ID for Micronaut MQTT clients in tests.
