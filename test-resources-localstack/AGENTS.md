# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources LocalStack aggregator: Modules for AWS service emulation (S3, SQS, SNS, DynamoDB) using LocalStack containers in tests.

## STRUCTURE
```
./test-resources-localstack/
├── test-resources-localstack-core/       # Shared logic, SPI, and base provider
├── test-resources-localstack-s3/         # S3-specific resolver
├── test-resources-localstack-sqs/        # SQS-specific resolver
├── test-resources-localstack-sns/        # SNS-specific resolver
└── test-resources-localstack-dynamodb/   # DynamoDB-specific resolver
```

## WHERE TO LOOK
- Core SPI & provider → test-resources-localstack-core/src/main/java/io/micronaut/testresources/localstack/LocalStackService.java and LocalStackTestResourceProvider.java
- Service implementations → Each submodule's src/main/java/io/micronaut/testresources/localstack/<service>/LocalStack<Service>Service.java (e.g., LocalStackS3Service)
- Test fixtures → test-resources-localstack-core/src/testFixtures/groovy/io/micronaut/testresources/localstack/AbstractLocalStackSpec.groovy
- Build config → Each submodule's build.gradle for dependencies and conventions

## CODE MAP
Java-based with Groovy tests. Core uses ServiceLoader to aggregate services. Each service module implements LocalStackService for property resolution.

## CONVENTIONS
- Property namespaces: aws.services.<service>.endpoint-override (lowercase, e.g., s3, sqs)
- Shared credentials: aws.access-key-id, aws.secret-key, aws.region resolved centrally
- Container management: Single shared LocalStackContainer with multiple services enabled
- Dependencies: AWS SDK v1 at runtime (required by LocalStack), v2 for tests
- Testing: Spock specs in each module, extending AbstractLocalStackSpec

## ANTI-PATTERNS (THIS MODULE)
- Avoid tight coupling to AWS SDK versions; current runtime dependency on v1 is necessary but monitor for upgrades
- Don't duplicate endpoint/credential logic per-service; use core provider's shared mapping
- Preserve modular structure: Core handles aggregation, services focus on specifics

## UNIQUE STYLES
- Aggregator pattern with core + per-service submodules sharing SPI
- Test fixtures for reusable LocalStack test setup
- Explicit runtime deps on AWS SDK v1 with justifications in build files

## COMMANDS
```bash
./gradlew :micronaut-test-resources-localstack:build    # Build all LocalStack modules
./gradlew :micronaut-test-resources-localstack:check    # Run tests across modules
```

## NOTES
- Shares logic via core module; extend AbstractLocalStackSpec for new service tests
- Properties resolved via container's getEndpointOverride(service)
- No main entrypoint; consumed as test dependency in Micronaut apps
