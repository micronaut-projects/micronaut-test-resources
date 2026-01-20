# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T00:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
test-resources-localstack-s3: Module providing LocalStack-based resolver for AWS S3 test resources in Micronaut. 
It implements a service to spin up a LocalStack container and resolve the S3 endpoint override property, 
enabling seamless testing with emulated S3 without real AWS dependencies.

## STRUCTURE
```
./test-resources-localstack-s3/
├── build.gradle                          # Module build configuration and dependencies
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/localstack/s3/LocalStackS3Service.java  # Core service class
│   │   └── resources/META-INF/services/io.micronaut.testresources.localstack.LocalStackService  # SPI registration
│   └── test/
│       └── groovy/io/micronaut/testresources/localstack/s3/LocalStackS3Test.groovy  # Spock-based integration tests
└── .settings/                            # Eclipse/IDE configuration files (can be ignored)
```

## WHERE TO LOOK
- S3 Resolver Logic → src/main/java/io/micronaut/testresources/localstack/s3/LocalStackS3Service.java
  (Implements LocalStackService interface for property resolution)
- Testing Examples → src/test/groovy/io/micronaut/testresources/localstack/s3/LocalStackS3Test.groovy
  (Demonstrates container startup, property injection, and S3 client usage with AWS SDK v2)
- Dependency Setup → build.gradle (depends on test-resources-localstack-core and AWS SDK)

## CODE MAP
Small module with single main class extending abstract LocalStack support. 
Key methods:
- resolve(): Returns LocalStack S3 endpoint URL for \"aws.services.s3.endpoint-override\"
- getService(): Specifies LocalStackContainer.Service.S3
- getProperties(): Lists resolvable properties (only endpoint override)

No complex architecture; integrates with broader LocalStack aggregator.

## CONVENTIONS
- Property Namespaces: Uses \"aws.services.s3.endpoint-override\" for resolved endpoint URL.
- Bucket Properties: Does NOT automatically create or resolve bucket names/props; tests must manually create buckets via AWS SDK (e.g., S3Client.createBucket() as in tests).
- Container Management: Relies on shared LocalStack container from core; use getEndpointOverride(LocalStackContainer.Service.S3) for URLs.
- Testing: Extend AbstractLocalStackSpec for specs; bind resolved props to config classes with @ConfigurationProperties.
- Ephemeral Ports: Always use dynamic ports from container; resolved via endpoint override.

## ANTI-PATTERNS (THIS MODULE)
- Hard-coding Endpoints: Avoid manually setting S3 URLs in tests; always resolve via Test Resources to ensure portability.
- Bucket Auto-Creation: Don't assume buckets are pre-created; module only handles endpoint, not resource provisioning (handle in test setup).
- Multiple Containers: Prevent starting duplicate LocalStack instances; service ensures singleton via core resolver SPI.
- Blocking Operations: In tests, avoid sync AWS calls that block Micronaut's event loop if using reactive stacks.
- Ignoring Service Registration: Ensure META-INF/services file is present for proper SPI discovery.

## UNIQUE STYLES
- Groovy/Spock Tests: All tests in Groovy with Spock framework, following project-wide pattern.
- Minimalist Implementation: Class is ~30 lines, focusing solely on S3 endpoint resolution without additional features.

## COMMANDS
```bash
cd ../test-resources-localstack/test-resources-localstack-s3
../../gradlew build              # Build module
../../gradlew check              # Run tests
../../gradlew publishToMavenLocal # Publish locally
```

## NOTES
- Part of test-resources-localstack aggregator; see parent AGENTS.md for broader context.
- No direct entry points; loaded via SPI in test-resources-localstack-core.
- Tests verify single container usage and basic S3 operations (bucket create/list/put/get).
- For bucket-specific props, consider custom resolvers or manual setup in tests.
