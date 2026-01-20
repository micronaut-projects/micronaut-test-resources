# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T00:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources module providing LocalStack-based SQS emulation for integration tests. Automatically provisions a LocalStack container and resolves the SQS endpoint override property, enabling seamless testing of AWS SQS interactions without real AWS dependencies.

This submodule is part of the test-resources-localstack aggregator, focusing exclusively on SQS service resolution.

## STRUCTURE
```
./test-resources-localstack-sqs/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   ├── java/.../LocalStackSQSService.java  # Core SQS resolver implementation
│   │   └── resources/META-INF/services/...     # Service loader for LocalStackService
│   └── test/
│       ├── groovy/.../LocalStackSQSTest.groovy # Spock integration tests
│       └── resources/logback.xml               # Test logging config
└── .settings/ .classpath .project .factorypath  # IDE configuration files
```

## WHERE TO LOOK
- SQS property resolution logic → src/main/java/io/micronaut/testresources/localstack/sqs/LocalStackSQSService.java
- Shared LocalStack abstractions → ../test-resources-localstack-core/src/main/java/io/micronaut/testresources/localstack/
- Test demonstrations (queue creation, messaging) → src/test/groovy/io/micronaut/testresources/localstack/sqs/LocalStackSQSTest.groovy
- Build and dependencies → build.gradle (extends core LocalStack conventions)

## CODE MAP
Minimal codebase: Single main class (LocalStackSQSService) implementing the LocalStackService interface from core. Tests use Spock for verifying endpoint resolution and basic SQS operations via AWS SDK.

## CONVENTIONS
- **Queue Properties**: Expose properties under the `aws.services.sqs.*` namespace, primarily `endpoint-override` for LocalStack's emulated endpoint. Resolved dynamically via `LocalStackContainer.getEndpointOverride(Service.SQS)`.
- Follow project-wide patterns: Use ephemeral ports; integrate with Testcontainers for container management; register via META-INF/services for auto-discovery.
- Property names are standardized to match AWS SDK expectations, ensuring compatibility with Micronaut AWS modules.

## ANTI-PATTERNS (THIS MODULE)
- Do NOT handle queue lifecycle (create/delete) within the resolver—delegate to tests using AWS SDK for realism and flexibility.
- Avoid hard-coding endpoints or ports; always derive from container's runtime configuration to prevent conflicts.
- Refrain from tight coupling to specific AWS SDK versions beyond what's required in build.gradle; rely on core for shared dependencies.
- Don't duplicate container startup logic—extend from test-resources-localstack-core to maintain modularity.

## UNIQUE STYLES
- Leverages AWS SDK's SqsClient for test interactions, emphasizing endpoint injection over full service mocking.
- Tests are Spock-based, focusing on property resolution and end-to-end messaging scenarios.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-localstack-sqs:build    # Build and test this module
./gradlew :micronaut-test-resources-localstack-sqs:check    # Run tests
./gradlew :micronaut-test-resources-localstack-sqs:publishToMavenLocal
```

## NOTES
- Depends on test-resources-localstack-core for base LocalStack integration.
- Ideal for testing Micronaut applications using AWS SQS without cloud costs or credentials.
- Integration tests demonstrate real-world usage: creating queues, sending/receiving messages via resolved endpoint.
