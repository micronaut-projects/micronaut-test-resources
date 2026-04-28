# Build And Test

Use the Gradle wrapper and the standardized project names produced by Micronaut build conventions. The root project name is `testresources-parent`, while Gradle project paths use the `:micronaut-test-resources-*` prefix.

## Verification Scope

- For guidance-only changes, use a readability check plus `git diff --check`.
- For one module, prefer the module's targeted `check` task.
- For shared SPI, Testcontainers base classes, build logic, or server/client contracts, run the affected module checks and at least one representative dependent module.
- Use full `./gradlew check` only when a change crosses broad project boundaries or when requested.

## Common Commands

```bash
./gradlew check
./gradlew :micronaut-test-resources-core:check
./gradlew :micronaut-test-resources-server:check
./gradlew :micronaut-test-resources-jdbc-core:check
./gradlew publishGuide
./gradlew docs
```

`publishToMavenLocal` is useful when validating this project from an external sample application:

```bash
./gradlew publishToMavenLocal
```

## Test Layout

- Most tests are Spock specs under `src/test/groovy`.
- Some modules define additional suites, such as `test-resources-embedded` with `test2`.
- `test-resources-extensions/test-resources-extensions-junit-platform` has extra JVM test suites and Kotlin/KSP wiring.
- Shared provider modules commonly expose test fixtures; prefer those fixtures over duplicating setup logic.
- Container-backed tests require Docker and may be slower or environment-sensitive.

## Build Logic Notes

- `settings.gradle` declares module families and imports Micronaut version catalogs.
- `buildSrc/src/main/groovy/io.micronaut.build.internal.*.gradle` contains internal conventions for provider families and server variants.
- Keep Gradle configuration cache and dependency resolution behavior in mind: do not introduce network work during configuration.
