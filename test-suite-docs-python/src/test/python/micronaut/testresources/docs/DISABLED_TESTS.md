# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples of Micronaut Test Resources under
`test-suite-docs-python/src/test/python/micronaut/testresources/docs` that are disabled, or that carry a workaround because
the direct port of the Java example does not compile or does not behave like the Java example yet (Python compiler gaps).
It is the bug-fixing task list for the Python compiler (`micronaut-inject-python` / `micronaut-context-python`); every row
references a `TODO(python)` comment in the sources.

The Python examples are compiled by every build and their tests run with `./gradlew pythonCheck -Ppython-ci`
(the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 2.
- Last generated command: `rg -n "@Disabled\(" test-suite-docs-python/src/test/python`.
- Last full-suite command: `./gradlew :test-suite-docs-python:test -Ppython-ci`.
- Last full-suite result (micronaut-core 5.2.3 / micronaut-build 8.1.2): build successful, 8 tests executed (8 test classes), 2 skipped.

## Migration Rules

- The snippet classes live in `io.micronaut.testresources.docs.<topic>` in every language (`project-base="test-suite-docs"`
  in the guide), so the Python package is `micronaut.testresources.docs.<topic>`.
- The suites run the Micronaut Test extensions in-process: no test resources service is started. The JUnit Platform
  listener `io.micronaut.testresources.docsupport.DocsTestResourcesClientInjector` (Java, `src/test/java`, outside the
  Python package tree) installs a fake `TestResourcesClient` resolving the `rabbitmq.uri` property of the
  `@TestResourcesProperties` example; the `@TestResourcesScope` examples read the current scope from `ScopeHolder`, the
  `@Value` examples get their properties from `@Property` on the test class.
- JUnit annotations and the annotations of `io.micronaut.test.extensions.junit5.annotation` (`@MicronautTest`,
  `@TestResourcesScope`, including its class-valued `namingStrategy=ScopeNamingStrategy.TestClassName` member) are copied
  onto the generated test class and work; the scope name computed by `ScopeNamingStrategy.TestClassName` is the name of
  the generated class (`micronaut.testresources.docs.junitplatform.ScopeNamingStrategyTest`).
- A Python test class is a `@MicronautTest` with `@Test` methods and plain `assert` statements.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `micronaut.testresources.docs.core.MyTest` | `TestPropertyProvider.getProperties()` is called by Micronaut Test before the application context, and with it the GraalPy runtime, exists: the generated `getProperties()` bridge fails with `IllegalStateException: GraalPy context has not been initialized` (`PythonApplicationRuntime.require` <- `PythonContextRuntime.newInstance` <- `MyTest.asPolyglotValue`), so a Python test class cannot supply test properties this way (still fails with micronaut-core 5.2.3). The guide renders the snippet for Java, Kotlin and Groovy with a `[.lang-python]` note. |
| `micronaut.testresources.docs.core.ConnectionSpec` | `@TestResourcesProperties` (`io.micronaut.test.extensions.testresources.annotation`) is read by reflection from the test class (`TestResourcesPropertiesFactory`: `testClass.getAnnotation(TestResourcesProperties.class)`), but the Python compiler only copies the JUnit annotations, `@MicronautTest` and the `@ExtendWith`-derived test annotations onto the generated class; other `io.micronaut` annotations are served by the annotation metadata and are not subject to the reflection option (`-Amicronaut.introspection.allowReflection=micronaut.testresources.docs.*` was tried with micronaut-core 5.2.3: the generated class still carries only `@MicronautTest`), so `rabbitmq.uri` is never requested from test resources and the `${rabbitmq.servers.product-cluster.port}` placeholder fails to resolve (`DependencyInjectionException: Error resolving property value [${rabbitmq.servers.product-cluster.port}]. Property doesn't exist`). Once copied, the `RabbitMQProvider` class would still be instantiated by reflection (`getDeclaredConstructor().newInstance()`) before the GraalPy runtime exists. The guide renders both snippets for Java, Kotlin and Groovy with a `[.lang-python]` note. |

## Workarounds in the Sources

None.

## `java.type` usages

None.
