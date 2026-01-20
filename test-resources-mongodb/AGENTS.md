# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:22:23Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Provides MongoDB test resources using Testcontainers, integrating with Micronaut's test-resources-core for automatic provisioning of MongoDB instances during tests. Focuses on synchronous driver support.

## STRUCTURE
```
./test-resources-mongodb/
├── build.gradle                          # Module build configuration
├── src/main/java/io/micronaut/testresources/mongodb/MongoDBTestResourceProvider.java  # Core provider implementation
├── src/test/groovy/io/micronaut/testresources/mongodb/  # Spock integration tests (e.g., MultipleMongoDBTest.groovy, CustomDbNameMongoDBTest.groovy)
└── src/test/resources/                   # Test configuration YAML files
```

## WHERE TO LOOK
- MongoDB resource provider logic → package io.micronaut.testresources.mongodb.*

## CODE MAP
Compact module centered on MongoDBTestResourceProvider, extending AbstractTestContainersProvider for Testcontainers integration.

## CONVENTIONS
- Resolves connection string properties: "mongodb.uri" and "mongodb.servers.<name>.uri".
- Starts containers as single-node replica sets (via getReplicaSetUrl).
- Default image tag: "mongo:5"; override with test resources configuration.
- Supports custom database name via "containers.mongodb.db-name".

## ANTI-PATTERNS (THIS PROJECT)
- Avoid mixing reactive drivers here; use dedicated reactive modules (e.g., test-resources-mongodb-reactive) for reactive MongoDB support.

## UNIQUE STYLES
- Leverages Testcontainers' MongoDBContainer for ephemeral database instances.
- Supports multiple named servers for advanced test scenarios.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-mongodb:build   # Build the module
./gradlew :micronaut-test-resources-mongodb:test    # Run Spock tests
./gradlew :micronaut-test-resources-mongodb:publishToMavenLocal  # Publish locally
```

## NOTES
- All tests are Spock specifications; extend AbstractMongoDBSpec for common setup.
- Example tests demonstrate features: MultipleMongoDBTest for multi-server, MongoDBStartedTest for basic startup, CustomDbNameMongoDBTest for db name config.
- Avoid aggregator-level docs; focus on MongoDB-specific implementation details.
