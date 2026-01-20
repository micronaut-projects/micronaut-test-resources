# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fdd0becc33eeaa1912b28b850da5aca59c
Branch: kafka-native

## OVERVIEW
Standalone Micronaut application serving test resources remotely via REST API.

## STRUCTURE
```
test-resources-server/
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/server/  # Core classes: TestResourcesService, TestResourcesController, etc.
│   │   └── resources/application.properties         # Configuration
│   └── aot/                                        # AOT optimizations
└── build.gradle.kts                                # Module build script
```

## WHERE TO LOOK
- Entry point → src/main/java/io/micronaut/testresources/server/TestResourcesService.java (contains the sole main method in the repo, starts the Micronaut application)
- Controllers → TestResourcesController.java (@Controller(\"/\") handling all endpoints)
- HTTP Endpoints:
  - GET/POST /list: Lists resolvable properties (with or without config)
  - POST /resolve: Resolves a specific property value
  - GET /requirements/expr/{expression}: Lists required properties for an expression
  - GET /requirements/entries: Lists all required property entries
  - GET /close/all: Closes all test resources
  - GET /close/{id}: Closes resources for a specific scope
  - GET /testcontainers: Lists all started test containers
  - GET /testcontainers/{scope}: Lists containers for a scope
  - POST /stop: Requests server shutdown

## CODE MAP
- TestResourcesService: Application startup, shutdown handling, periodic expiry check
- TestResourcesController: REST API implementation, resolver delegation, container management
- ExpiryManager: Handles server inactivity timeout
- CompositeResolverLoader: Loads and manages test resource resolvers
- AccessFilter/AccessConfiguration: Security for API access
- PropertyResolutionListener: Hooks for resolution events

## CONVENTIONS
- AOT optimizations stored under src/aot/ (e.g., aot.properties for GraalVM native image config)
- Configuration in application.properties: application name, random port (-1), control panel enabling
- Uses Micronaut features: @Scheduled for timeouts, @ExecuteOn for blocking tasks
- Resolvers must implement TestResourcesResolver interface
- Test resources config keys should not include 'test-resources.' prefix (sanitized if present)

## ANTI-PATTERNS (THIS MODULE)
- Avoid blocking operations on Micronaut event loop; use @ExecuteOn(TaskExecutors.BLOCKING) for I/O or long-running tasks
- Do not hard-code ports; always use ephemeral/random ports for containers and server
- Avoid direct TestContainers usage outside resolvers; use abstraction layers
- Do not add blocking code in controllers without proper executor
- Preserve non-blocking nature for high throughput

## UNIQUE STYLES
- Asynchronous shutdown with task scheduler and max timeout
- Parallel closing of resolvers during shutdown
- Sanitization of test resources config to remove prefixes
- Integration with testcontainers for resource lifecycle management
- Optional control panel integration via config

## COMMANDS
```bash
./gradlew :micronaut-test-resources-server:build   # Build the server module
./gradlew :micronaut-test-resources-server:run     # Run the server locally
./gradlew :micronaut-test-resources-server:test    # Run module tests
```

## NOTES
- Sole module with a main method; acts as remote server for test resource provisioning
- Used by control-panel for UI and client for API access
- Supports scoped resource management for multiple test contexts
- Automatic shutdown on inactivity via ExpiryManager
- Heavy use of Testcontainers; ensure Docker is available for development/testing
"
