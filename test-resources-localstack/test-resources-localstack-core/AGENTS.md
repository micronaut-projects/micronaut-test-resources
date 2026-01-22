# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Provides core support for launching LocalStack test containers in Micronaut Test Resources. This module handles the shared logic for AWS service emulation using LocalStack, including container management and property resolution for services like S3, SQS, SNS, and DynamoDB.

## STRUCTURE
```
./test-resources-localstack-core/
├── src/main/java/io/micronaut/testresources/localstack/   # Core classes
│   ├── LocalStackService.java                             # SPI interface
│   └── LocalStackTestResourceProvider.java                # Main provider
├── src/testFixtures/groovy/                               # Test helpers
│   └── io/micronaut/testresources/localstack/AbstractLocalStackSpec.groovy
└── build.gradle                                           # Build config
```

## WHERE TO LOOK
- Core SPI & interface → src/main/java/io/micronaut/testresources/localstack/LocalStackService.java
- Container management & property resolution → src/main/java/io/micronaut/testresources/localstack/LocalStackTestResourceProvider.java
- Test fixtures for specs → src/testFixtures/groovy/io/micronaut/testresources/localstack/AbstractLocalStackSpec.groovy
- Build dependencies (e.g., Testcontainers LocalStack) → build.gradle

## CODE MAP
Primarily Java with Groovy test fixtures. Uses Java's ServiceLoader to discover LocalStackService implementations from sibling modules. Integrates with micronaut-test-resources-testcontainers for abstract container handling.

## CONVENTIONS
- Property namespaces: Use aws.services.<service-name-lowercase>.* (e.g., aws.services.s3.endpoint-override, aws.services.sqs.endpoint-override)
- Shared credentials & config: aws.access-key-id, aws.secret-key, aws.region are resolved centrally from the LocalStack container
- Endpoint props: Services provide endpoint overrides via getEndpointOverride method
- Container sharing: Single LocalStackContainer instance with multiple services enabled via withServices

## ANTI-PATTERNS (THIS PROJECT)
- Tight SDK coupling: Runtime dependency on AWS SDK v1 (required by LocalStack); avoid direct SDK calls in resolvers—use container abstractions. Monitor LocalStack upgrades for SDK v2 support.
- Duplication of logic: Don't replicate endpoint/credential resolution in service modules; leverage core provider's shared mapping and delegation.
- Hard-coded configs: Prefer dynamic resolution over fixed ports or creds.

## UNIQUE STYLES
- SPI-based extension: LocalStackService interface allows modular addition of new AWS services without modifying core.
- Test fixtures: AbstractLocalStackSpec provides reusable base for Spock tests in aggregator submodules.
- Minimalist design: Focuses on resolution logic, deferring specifics to implementers.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-localstack-core:build  # Build the core module
./gradlew :micronaut-test-resources-localstack-core:check  # Run tests (Spock specs)
./gradlew publishToMavenLocal                    # Publish locally for testing
```

## NOTES
- Depends on testcontainers-localstack for container orchestration.
- Properties resolved using container.getEndpointOverride(service) for consistency.
- No main application; consumed as a dependency by LocalStack service modules (e.g., -s3, -sqs).
- Ensure ServiceLoader META-INF/services entry for TestResourcesResolver.
