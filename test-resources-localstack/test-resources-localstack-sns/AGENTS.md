# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:00:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources module providing Amazon SNS (Simple Notification Service) emulation using LocalStack for automated test resource provisioning. Integrates with the LocalStack aggregator to supply SNS endpoints via a shared container, enabling seamless testing of SNS-dependent Micronaut applications without real AWS.

## STRUCTURE
```
./test-resources-localstack-sns/
├── build.gradle                          # Gradle build configuration and dependencies
├── src/main/java/io/micronaut/testresources/localstack/sns/  
│   └── LocalStackSNSService.java         # Core service implementation
├── src/main/resources/META-INF/services/ 
│   └── io.micronaut.testresources.localstack.LocalStackService  # ServiceLoader config
├── src/test/groovy/io/micronaut/testresources/localstack/sns/
│   ├── LocalStackSNSTest.groovy          # Spock integration test
│   ├── TestSnsConfig.groovy              # Test configuration bindings
│   └── TestSnsClientFactory.groovy       # SNS client factory for tests
└── src/test/resources/                   
    └── logback.xml                       # Test logging config
```

## WHERE TO LOOK
- **Core Implementation**: src/main/java/io/micronaut/testresources/localstack/sns/LocalStackSNSService.java – Handles SNS service resolution and endpoint overriding.
- **Service Registration**: src/main/resources/META-INF/services/io.micronaut.testresources.localstack.LocalStackService – Registers the SNS service via ServiceLoader for aggregation in LocalStack core.
- **Tests**: src/test/groovy/io/micronaut/testresources/localstack/sns/LocalStackSNSTest.groovy – Verifies container startup, topic creation, and listing using AWS SDK.
- **Build Config**: build.gradle – Defines dependencies (e.g., AWS SDK v1 for runtime, v2 for tests) and applies LocalStack module plugin.
- **Parent Aggregator**: ../test-resources-localstack-core/ – Shared logic for container management and property resolution.

## CODE MAP
Compact Java module with Groovy/Spock tests. Implements LocalStackService SPI to resolve SNS-specific properties. Uses AWS SDK for client interactions in tests. No complex architecture; focuses on endpoint provision.

## CONVENTIONS
- **Property Namespaces**: Follows 'aws.services.sns.*' prefix. Key property: 'aws.services.sns.endpoint-override' resolved to LocalStack container's SNS endpoint.
- **Topic Properties**: SNS topics are not auto-provisioned; use AWS SDK (SnsClient) to create/list topics dynamically in tests. ARNs follow LocalStack format (e.g., 'arn:aws:sns:us-east-1:000000000000:test-topic'). Shared credentials (aws.access-key-id, aws.secret-key, aws.region) are resolved centrally via LocalStack core.
- **Container Sharing**: Relies on single shared LocalStackContainer; enable SNS via LocalStackContainer.Service.SNS.
- **Dependencies**: Runtime: AWS SDK v1 (com.amazonaws:aws-java-sdk-sns) due to LocalStack requirements. Tests: AWS SDK v2 (software.amazon.awssdk:sns).
- **Testing**: Extends AbstractLocalStackSpec for Spock tests; inject SnsClient with endpoint override.

## ANTI-PATTERNS (THIS MODULE)
- Do NOT duplicate endpoint or credential resolution; leverage LocalStack core's shared mapping to avoid redundancy.
- Avoid hard-coding AWS SDK versions or direct container instantiation; use SPI and aggregator for modularity.
- Do NOT create per-service containers; always use the shared LocalStackContainer to prevent resource overhead.
- Steer clear of AWS SDK v2 at runtime (use v1 as mandated by LocalStack); monitor for LocalStack upgrades supporting v2.
- Preserve minimalism: Do not add topic auto-creation logic here; handle in application tests via SDK.

## UNIQUE STYLES
- SPI-driven registration via ServiceLoader for easy aggregation.
- Mixed SDK versions: v1 runtime for compatibility, v2 in tests for modern features.
- Lightweight: Single implementation class, focused on property resolution.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-localstack-sns:build  # Build this module
./gradlew :micronaut-test-resources-localstack-sns:check  # Run tests
./gradlew publishToMavenLocal                             # Publish for local use
```

## NOTES
- Integrates with test-resources-localstack aggregator; no standalone usage.
- Topics must be created via SDK calls (e.g., createTopic); module only provides endpoint.
- Test with @MicronautTest and injected SnsClient for endpoint-override awareness.
- Monitor AWS SDK compatibility as LocalStack evolves.
