# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fdd0becc33eeaa1912b28b850da5aca59c
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Client: HTTP client facade for resolving test properties from remote or embedded resolvers.

## STRUCTURE
```
./test-resources-client/
├── src/main/java/io/micronaut/testresources/client/  # Core client classes and interfaces
│   ├── DefaultTestResourcesClient.java
│   ├── NoOpClient.java
│   ├── TestResourcesClient.java
│   ├── TestResourcesClientFactory.java
│   ├── TestResourcesClientPropertyExpressionResolver.java
│   ├── TestResourcesClientPropertySourceLoader.java
│   ├── TestResourcesException.java
│   └── SimpleJsonErrorModel.java
├── src/main/resources/META-INF/services/            # Service loader configs
│   ├── io.micronaut.context.env.PropertyExpressionResolver
│   └── io.micronaut.context.env.PropertySourceLoader
├── src/test/groovy/                                 # Spock tests
│   └── io/micronaut/testresources/client/
└── build.gradle.kts                                 # Module build script
```

## WHERE TO LOOK
- Core client interface → src/main/java/io/micronaut/testresources/client/TestResourcesClient.java (HTTP-annotated methods for resolution)
- Default implementation → src/main/java/io/micronaut/testresources/client/DefaultTestResourcesClient.java (HttpClient-based requests)
- Factory and config → src/main/java/io/micronaut/testresources/client/TestResourcesClientFactory.java (creates clients from props/files)
- Error handling → src/main/java/io/micronaut/testresources/client/TestResourcesException.java and SimpleJsonErrorModel.java (exception throwing and JSON error parsing)
- Property resolution → src/main/java/io/micronaut/testresources/client/TestResourcesClientPropertyExpressionResolver.java (lazy resolution integration)
- No-op fallback → src/main/java/io/micronaut/testresources/client/NoOpClient.java (disabled mode)

## CODE MAP
Small module focused on HTTP client interactions; key pattern is Micronaut's @Client annotation on TestResourcesClient interface defining endpoints like /resolve, /list, /close/all. Implementation uses Java's HttpClient for synchronous requests with JSON handling via JsonMapper.

## CONVENTIONS
- Timeouts: Configurable via CLIENT_READ_TIMEOUT system property (default 60s); applied as connect and request timeouts in DefaultTestResourcesClient.
- Fallback: Uses NoOpClient returning empty/optional results when disabled or unconfigured; factory prioritizes system props over filesystem.
- Configuration: Enabled/disabled via test.resources.enabled prop; URI and access token via test.resources.server.uri and .access-token.
- Lazy loading: Clients are cached weakly and created on-demand to avoid early initialization issues.

## ANTI-PATTERNS (THIS MODULE)
- Avoid tight coupling to server internals: Use only the defined TestResourcesClient interface methods; do not assume specific server responses beyond documented JSON formats.
- Do not hard-code endpoints or headers; rely on @Client annotations and factory configuration.
- Steer clear of direct HttpClient usage outside DefaultTestResourcesClient to maintain abstraction.

## UNIQUE STYLES
- Integrates as PropertyExpressionResolver and PropertySourceLoader via META-INF services for seamless Micronaut context injection.
- Supports optional access token for authenticated requests without exposing auth logic.
- Error messages are sanitized to remove stack traces in exceptions.

## COMMANDS
```bash
cd test-resources-client
../gradlew build    # Build and test module
../gradlew check    # Run tests
../gradlew publishToMavenLocal  # Local publish
```

## NOTES
- Primarily used by test extensions (e.g., JUnit platform) and embedded resolvers to fetch dynamic properties like DB URLs/credentials.
- No direct dependencies on server or container modules; acts as pure client facade.
- Tests use Spock for verifying client behavior without real server (mocked responses).