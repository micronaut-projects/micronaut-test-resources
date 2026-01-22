# PROJECT KNOWLEDGE BASE

Generated: 2026-01-22T12:14:22Z
Commit: da5801d0
Branch: kafka-native

## OVERVIEW
Extensions aggregator: Core test-resources annotations/property providers + JUnit Platform integration (bridges Micronaut Test Resources into test lifecycles).

## STRUCTURE
```
./test-resources-extensions/
├── test-resources-extensions-core/            # Annotations + property providers + client holder
└── test-resources-extensions-junit-platform/  # JUnit Platform adapter; Spock/Kotest source sets
```

## WHERE TO LOOK
- Core annotations & providers → test-resources-extensions-core/src/main/java/io/micronaut/test/extensions/testresources
- JUnit Platform bridge → test-resources-extensions-junit-platform/src/main/java/io/micronaut/test/extensions/junit5
- Custom test source sets → src/spockTest/groovy and src/koTest/kotlin (module-local)

## CONVENTIONS
- Exposes meta-annotations (e.g., @TestResourcesProperties) to attach required props to tests
- Test lifecycle integration done via JUnit Platform extension; frameworks use their adapter modules
- Source sets: uses non-standard spockTest/koTest to keep framework-specific tests isolated

## ANTI-PATTERNS (THIS DIRECTORY)
- Do not mix framework adapters in core; keep core framework-agnostic
- Do not perform I/O or container logic in extensions; only orchestration/metadata
- Avoid coupling to server internals; rely on client facades and SPI

## COMMANDS
```bash
./gradlew :micronaut-test-resources-extensions-core:check
./gradlew :micronaut-test-resources-extensions-junit-platform:check
```

## NOTES
- Both modules depend on test-resources-client and core SPI, no direct container deps here
- JUnit Platform adapter interoperates with Spock/Kotest via their runners
