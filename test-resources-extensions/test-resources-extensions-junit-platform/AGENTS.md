# MODULE KNOWLEDGE BASE

Generated: 2026-01-20T12:02:49Z
Commit: 8d7a43fdd0becc33eeaa1912b28b850da5aca59c
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources extension integrating test resources lifecycle with JUnit Platform; supports scoped resources for JUnit 5, Spock, and Kotest tests.

## STRUCTURE
```
./
├── build.gradle                          # Module build script defining custom JVM test suites (test, spockTest, koTest)
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/test/extensions/junit5/  # Core classes: listeners, annotations, scope holders
│   │   │   ├── annotation/               # Annotations like TestResourcesScope, ScopeNamingStrategy
│   │   │   └── (other files)             # TestResourcesScopeListener, SpockScopeExtension, etc.
│   │   └── resources/META-INF/
│   │       ├── services/                 # Service loaders for LauncherSessionListener, IGlobalExtension, TestPropertyProviderFactory
│   │       └── native-image/...          # Native image reflection config
│   ├── test/
│   │   ├── java/io/micronaut/test/extensions/junit5/  # JUnit 5 example tests (ClassScopeTest, InheritedScopeTest, etc.)
│   │   └── resources/                    # Test config like application-test.yml, logback.xml
│   ├── spockTest/
│   │   └── groovy/io/micronaut/test/extensions/junit5/  # Spock example tests (BasicScopeTest, InheritedScopeTest, etc.)
│   ├── koTest/
│   │   └── kotlin/io/micronaut/test/extensions/junit5/  # Kotest example tests (ClassScopeTest, InheritedScopeTest, etc.)
│   └── testFixtures/
│       ├── java/io/micronaut/test/extensions/testresources/junit5/  # Fixtures: FakeTestResourcesClient, FakeTestResourcesClientInjector
│       └── resources/META-INF/services/  # TestExecutionListener for fixtures
└── (build artifacts, .settings, etc.)     # IDE and build files
```

## WHERE TO LOOK
- Extension entry points → src/main/java/io/micronaut/test/extensions/junit5/TestResourcesLauncherSessionListener.java (implements LauncherSessionListener, registered via META-INF/services)
- Core listener → TestResourcesScopeListener.java (TestExecutionListener for scope management)
- Annotations → annotation/TestResourcesScope.java (@Inherited annotation for defining scopes with naming strategies)
- Spock integration → SpockScopeExtension.java (IGlobalExtension for @Shared field support in Spock)
- Scope naming → annotation/ScopeNamingStrategy.java (functional interface with implementations like PackageName, TestClassName)
- Test examples → src/test/java for JUnit, src/spockTest/groovy for Spock, src/koTest/kotlin for Kotest; focus on scope verification
- Build config → build.gradle for custom test source sets and dependencies

## CODE MAP
Skipped (small module; LSP not needed for navigation). Key namespace: io.micronaut.test.extensions.junit5.

## CONVENTIONS
- Spock tests under src/spockTest/groovy with dedicated 'spockTest' JVM test suite in build.gradle
- Kotlin tests under src/koTest/kotlin with dedicated 'koTest' JVM test suite in build.gradle
- JUnit 5 tests under src/test/java with standard 'test' suite
- Use @TestResourcesScope for scoping; supports inheritance from classes, interfaces, traits
- Service loader registration for extensions and listeners
- Test fixtures in src/testFixtures for reusable fake clients

## ANTI-PATTERNS (THIS MODULE)
- Mixing test scopes across frameworks (e.g., running Spock and JUnit in same suite) – use isolated test suites
- Overriding scopes without considering inheritance order in multiple inheritance scenarios
- Hard-coding scope names; always use naming strategies for consistency
- Forgetting to close scopes in tests, leading to resource leaks (tests verify all scopes closed)

## UNIQUE STYLES
- Deep integration with JUnit lifecycle via LauncherSessionListener and TestExecutionListener
- Scope inheritance and overriding support across Java/Groovy/Kotlin (e.g., from traits/interfaces)
- Custom naming strategies for flexible scope identification (package, class-based)
- Extensive test coverage for edge cases like multiple inheritance, setup/shared annotations in Spock

## COMMANDS
```bash
./gradlew :micronaut-test-resources-extensions:test-resources-extensions-junit-platform:build  # Build module
./gradlew :micronaut-test-resources-extensions:test-resources-extensions-junit-platform:test   # Run JUnit tests
./gradlew :micronaut-test-resources-extensions:test-resources-extensions-junit-platform:spockTest  # Run Spock tests
./gradlew :micronaut-test-resources-extensions:test-resources-extensions-junit-platform:koTest     # Run Kotest tests
./gradlew :micronaut-test-resources-extensions:test-resources-extensions-junit-platform:check      # Run checks including tests
```

## NOTES
- Module focuses on scoped test resources: class-level, package-level, inherited; many tests verify scope creation, reuse, and closure
- Uses FakeTestResourcesClient for isolated testing without real server
- Compatible with Micronaut's test ecosystem; extends core test-resources SPI
- No main application; purely an extension module with heavy test emphasis
- Potential hotspots: Scope resolution logic in TestResourcesScopeListener (handles annotations, inheritance)
