# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T13:00:00Z
Commit: e716b3e6
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources LocalStack DynamoDB module: Provides DynamoDB endpoint resolution using LocalStack containers for test environments.

## STRUCTURE
```
./test-resources-localstack-dynamodb/
├── build.gradle                          # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/localstack/dynamodb/LocalStackDynamoDBService.java  # Core service implementation
│   │   └── resources/META-INF/services/io.micronaut.testresources.localstack.LocalStackService  # Service loader configuration
│   └── test/
│       ├── groovy/io/micronaut/testresources/localstack/dynamodb/LocalStackDynamoDBTest.groovy  # Integration tests
│       └── resources/logback.xml       # Test logging config
└── .settings/                            # IDE configuration files
```

## WHERE TO LOOK
- Service resolution logic → src/main/java/io/micronaut/testresources/localstack/dynamodb/LocalStackDynamoDBService.java
- Integration tests → src/test/groovy/io/micronaut/testresources/localstack/dynamodb/LocalStackDynamoDBTest.groovy
- Build and deps → build.gradle (depends on localstack-core, AWS SDK v1/v2)
- Property mapping → AWS_DYNAMODB_ENDPOINT_OVERRIDE = \"aws.services.dynamodb.endpoint-override\"

## CODE MAP
Single Java class implementing LocalStackService for DynamoDB-specific endpoint override. Uses Groovy/Spock for testing, extending AbstractLocalStackSpec from core. Service loader via META-INF.

## CONVENTIONS
- Property namespace: aws.services.dynamodb.*
- Resolved properties: endpoint-override (maps to container's DynamoDB endpoint)
- Service enablement: LocalStackContainer.Service.DYNAMODB
- Dependencies: Runtime AWS SDK v1 (LocalStack requirement), test AWS SDK v2
- Testing: Spock specs verifying table ops and container startup

## ANTI-PATTERNS (THIS MODULE)
- Avoid duplicating credential/region logic; inherit from localstack-core
- Don't use AWS SDK v2 at runtime; stick to v1 for LocalStack compatibility
- Prevent hard-coded ports/endpoints; always use container-provided values

## UNIQUE STYLES
- Minimalist service impl: Only overrides resolveProperty, getServiceKind, getResolvableProperties
- Test uses @MicronautTest and builds DynamoDbClient to validate functionality
- Shares container management with aggregator core

## COMMANDS
```bash
./gradlew :micronaut-test-resources-localstack-dynamodb:build   # Build module
./gradlew :micronaut-test-resources-localstack-dynamodb:check   # Run tests
./gradlew :micronaut-test-resources-localstack-dynamodb:publishToMavenLocal
```

## NOTES
- Part of localstack aggregator; consumes shared LocalStackContainer
- Focuses solely on DynamoDB; no additional services or complex logic
- Test asserts single container instance and basic CRUD operations