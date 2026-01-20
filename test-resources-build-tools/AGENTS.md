# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T13:00:00Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Provides build-time utilities for managing the Micronaut Test Resources server, including server lifecycle, classpath inference, and integration with build processes.

## STRUCTURE
```
./test-resources-build-tools/
├── build.gradle                          # Module build script; generates VersionInfo and KnownModules
├── src/main/java/io/micronaut/testresources/buildtools/
│   ├── ServerUtils.java                  # Core server management (703 lines)
│   ├── ServerFactory.java                # Interface for forking server processes
│   ├── TestResourcesClasspath.java       # Classpath inference utilities
│   ├── ServerSettings.java               # Server configuration holder
│   ├── MavenDependency.java              # Maven dependency representation
│   ├── ModuleIdentifier.java             # Module identification
│   ├── ClassDataSharingException.java    # CDS-related exceptions
│   └── KnownModules.java                 # Generated list of known modules
├── src/test/groovy/                      # Spock tests for utilities
│   ├── ServerUtilsTest.groovy
│   └── TestResourcesClasspathTest.groovy
└── build/                                # Generated classes (VersionInfo, KnownModules)
```

## WHERE TO LOOK
- Server lifecycle & process mgmt → ServerUtils.java (703 lines; handles start/stop, CDS optimization, settings persistence)
- Process forking abstraction → ServerFactory.java (interface for build tools to launch server)
- Classpath inference → TestResourcesClasspath.java (analyzes user deps to add test resources modules/drivers)
- Config persistence → ServerSettings.java (port, token, timeouts)
- Dependency modeling → MavenDependency.java & ModuleIdentifier.java
- Generated artifacts → build/version-info/VersionInfo.java & build/module-list/KnownModules.java (from build tasks)

## CODE MAP
Java-based utilities with generated classes; small codebase (~1.5k LoC total). Key complexity in ServerUtils (process handling, CDS) and TestResourcesClasspath (dependency pattern matching). Tests in Groovy/Spock.

## CONVENTIONS
- No network operations in Gradle configuration phase; all resolution cache-friendly.
- Use generated KnownModules for module lists instead of hardcoding.
- Prefer Java process forking over external commands for server startup.
- Classpath inference uses pattern matchers for flexibility and extensibility.

## ANTI-PATTERNS (THIS PROJECT)
- Shelling out from Gradle tasks (e.g., avoid Runtime.exec for non-Java processes; use ServerFactory instead).
- Hardcoding module/dependency lists; rely on generated KnownModules.
- Introducing runtime dependencies in build logic; keep buildSrc clean.

## UNIQUE STYLES
- Heavy use of Java records for data classes (e.g., ServerSettings, ProcessParameters).
- Generated sources (VersionInfo, KnownModules) integrated via custom Gradle tasks (WriteVersion, WriteModuleList).
- CDS support for JVM optimization, with fallback handling.
- Pattern-based classpath matching for inferring required test resources from user deps.

## COMMANDS
```bash
./gradlew :micronaut-test-resources-build-tools:build     # Build module
./gradlew :micronaut-test-resources-build-tools:test      # Run tests
./gradlew :micronaut-test-resources-build-tools:writeVersionClass  # Generate VersionInfo
./gradlew :micronaut-test-resources-build-tools:writeModuleList    # Generate KnownModules
```

## NOTES
- Integrates with test-resources-server by providing classpath and startup utils; consumed by build plugins.
- Focuses on build-time efficiency: CDS reduces server startup time in repeated test runs.
- No main entrypoint; purely a utility library for other modules/build logic.
- Tests cover server mocking and classpath inference scenarios.
- Avoid duplicating buildSrc logic; this module provides shared utils for server-related build tasks.
