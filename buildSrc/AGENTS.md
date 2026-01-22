# AGENTS.md: buildSrc Module Documentation

## OVERVIEW
Custom Gradle plugins and build logic for Micronaut Test Resources multi-module project.

## WHERE TO LOOK
- **Core Plugin Scripts**: src/main/groovy/io.micronaut.build.internal.*.gradle
  - io.micronaut.build.internal.server-module.gradle: Handles server-specific configurations including AOT attributes.
  - io.micronaut.build.internal.test-resources-base.gradle: Base setup for test resources modules.
  - io.micronaut.build.internal.test-resources-module.gradle: Extends base for full test resources functionality.
  - io.micronaut.build.internal.test-resources-simple-module.gradle: Simplified version for basic modules.
  - io.micronaut.build.internal.test-resources-mntest-extension.gradle: MNTest extension specifics.
  - io.micronaut.build.internal.testcontainers-module.gradle: Testcontainers integration.
  - io.micronaut.build.internal.jdbc-module.gradle: JDBC database testing.
  - io.micronaut.build.internal.r2dbc-module.gradle: R2DBC database testing.
  - io.micronaut.build.internal.hibernate-reactive-module.gradle: Hibernate Reactive support.
  - io.micronaut.build.internal.localstack-module.gradle: LocalStack for AWS services.
  - io.micronaut.build.internal.kafka-testing.gradle: Kafka testing setup.
  - io.micronaut.build.internal.database-testing.gradle: General database testing.
  - io.micronaut.build.internal.test-fixtures.gradle: Test fixtures configuration.
- **Build Configuration**: build.gradle - Defines dependencies for buildSrc (e.g., shadow plugin, micronaut-kotlin-build-plugins).
- **Generated Classes**: build/classes/java/main/ - Compiled plugins like IoMicronautBuildInternalServerModulePlugin.class.
- **Utility Classes**: src/main/groovy/io/micronaut/internal/ - Helpers like GenerateModuleList.java, ParallelGateService.java.

## CONVENTIONS
- **Plugin Hierarchy**: Plugins apply internal siblings (e.g., jdbc-module applies testcontainers-module and database-testing).
- **Version Catalogs**: Consistent dependency management (e.g., mn.micronaut.inject.java, libs.spock in test-resources-module).
- **Shadow Plugin Usage**: Applied in server-module for creating fat jars (e.g., optimized JIT variants).
- **Task Ordering**: Use afterEvaluate blocks to manage execution order, especially for AOT compatibility.
- **Attribute Preparation**: Custom methods like prepareAttributesForPublication in server-module for variant configurations.
- **Repositories**: Commented-out in base plugins; rely on project-level settings.
- **Dependencies**: Isolated to build logic; use providers for versions (e.g., micronaut-build-version).

## ANTI-PATTERNS
- **Dependency Leakage**: Never include application dependencies (e.g., Micronaut runtime libs) in buildSrc; restricts to build-time only.
- **Double afterEvaluate**: Avoid nested afterEvaluate; used sparingly in server-module as workaround for AOT plugin ordering issues – dangerous, can lead to unpredictable behavior.
- **Hardcoded Versions**: Always use version catalogs instead of direct version strings.
- **Overly Complex Variants**: Limit custom attributes to essentials; excessive variants complicate builds.

## AOT-RELATED ATTRIBUTES
- Defined in server-module.gradle: "io.micronaut.aot.optimized" attribute for jar variants.
- Prefers non-AOT optimized candidates by default.
- Configures shadowJar for AOT-optimized JIT jars.
- Uses AotOptimizedDisambiguationRule for resolution strategy.
- Comments note challenges with afterEvaluate ordering due to AOT plugins.

(End of documentation - 52 lines)