# Provider Modules

Providers expose Micronaut application properties by implementing the resolver SPI and, for container-backed services, reusing shared Testcontainers abstractions.

## Resolver Conventions

- Implement `TestResourcesResolver` and register implementations through `META-INF/services/io.micronaut.testresources.core.TestResourcesResolver`.
- Return `Optional.empty()` for properties a resolver does not support so other resolvers can handle them.
- Implement `getResolvableProperties()` and `getRequiredProperties()` accurately; avoid starting resources just to answer metadata questions.
- Use `Ordered` deliberately when resolver precedence matters.
- Respect scopes from `micronaut.test.resources.scope`; do not leak resources between scopes.

## Container Conventions

- Use Testcontainers dynamic ports and container-provided credentials.
- Keep provider default Docker images in `test-resources-core/src/main/resources/io/micronaut/testresources/core/default-images/Dockerfile`; provider code should consume `DefaultTestResourceImages` instead of hard-coding mutable image tags.
- Make image names and versions configurable through existing property namespaces when the module already supports overrides.
- When changing a provider default image, update the manifest, generated/runtime default wiring, and matching `src/main/docs/guide/*.adoc` default-image text together. The core drift test should fail if docs and runtime defaults diverge.
- Put shared behavior in the family core module instead of duplicating it in every database or LocalStack service module.
- Keep startup and shutdown lifecycle behavior centralized in existing abstractions where possible.

## Property Namespace Patterns

- JDBC providers resolve `datasources.{name}.*` values and use `containers.{db-type}.*` customizations.
- R2DBC providers resolve `r2dbc.datasources.{name}.*` values and reuse JDBC/container conventions where applicable.
- LocalStack providers share common LocalStack behavior in `test-resources-localstack-core` and expose service-specific properties from the service modules.
- Service modules should use namespaced, predictable properties rather than one-off keys.

## Database Family Rules

- Start in `test-resources-jdbc-core`, `test-resources-r2dbc-core`, or `test-resources-hibernate-reactive-core` before editing a concrete database module.
- Keep database-specific modules thin: image selection, driver-specific properties, and compatibility exceptions belong there; reusable lifecycle behavior belongs in core.
- Treat Oracle XE modules as compatibility paths. Prefer Oracle Free or Test Pilot paths for new work unless the issue is specifically about XE.
