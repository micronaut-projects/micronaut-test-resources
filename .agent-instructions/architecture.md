# Architecture Map

Micronaut Test Resources is organized around a small core SPI and many provider modules. Start with the shared module for the technology family before editing a provider-specific module.

## Shared Runtime And Build Modules

- `test-resources-core`: resolver SPI, scope model, lazy property source and expression integration.
- `test-resources-testcontainers`: base abstractions and test fixtures for container-backed providers.
- `test-resources-build-tools`: build-time helpers, classpath utilities, generated module metadata, and server utilities.
- `test-resources-client`: client API for remote server communication.
- `test-resources-embedded`: in-process test resources mode.
- `test-resources-server`: Micronaut application that serves test resources remotely. `TestResourcesService` is the main entry point.
- `test-resources-control-panel`: UI/control panel integration for the server.
- `test-resources-bom`: dependency management module.
- `buildSrc`: internal Gradle plugins and project conventions.

## Provider Families

- Database aggregators: `test-resources-jdbc/`, `test-resources-r2dbc/`, and `test-resources-hibernate-reactive/`.
- LocalStack aggregator: `test-resources-localstack/` with shared core plus service modules for S3, SQS, SNS, and DynamoDB.
- Single-provider modules include Azure, Couchbase, HashiCorp Consul/Vault, Hazelcast, HiveMQ, Infinispan, Kafka, MinIO, MongoDB, Neo4j, OAuth2, OpenSearch, Pulsar, RabbitMQ, Redis, SeaweedFS, Solr, and generic Testcontainers support.

`test-resources-elasticsearch` is present in the tree but currently disabled in `settings.gradle`; do not assume it participates in regular builds.

## Where To Look First

- Core resolution contract: `test-resources-core/src/main/java/io/micronaut/testresources/core/TestResourcesResolver.java`.
- Base container support: `test-resources-testcontainers/src/main/java/io/micronaut/testresources/testcontainers`.
- Server API and lifecycle: `test-resources-server/src/main/java/io/micronaut/testresources/server`.
- JDBC shared provider logic: `test-resources-jdbc/test-resources-jdbc-core`.
- R2DBC shared provider logic: `test-resources-r2dbc/test-resources-r2dbc-core`.
- Hibernate Reactive shared provider logic: `test-resources-hibernate-reactive/test-resources-hibernate-reactive-core`.
- LocalStack shared logic: `test-resources-localstack/test-resources-localstack-core`.
