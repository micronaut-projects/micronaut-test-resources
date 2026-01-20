# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: 8d7a43fd
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources: Gradle multi-module suite providing automatic test resources (databases, messaging, AWS emulation) via core resolvers, Testcontainers, and optional remote server.

## STRUCTURE
```
./
├── buildSrc/                             # Internal Gradle build logic
├── test-resources-core/                  # Resolver SPI and core model
├── test-resources-testcontainers/        # Base abstractions for container-backed providers
├── test-resources-server/                # Micronaut app (sole main) serving test resources remotely
├── test-resources-client/                # Client API to the server
├── test-resources-build-tools/           # Build-time helpers (classpath/server utils)
├── test-resources-extensions/
│   └── test-resources-extensions-junit-platform/  # JUnit Platform integration; Spock/Kotest-style tests
├── test-resources-jdbc/                  # Aggregator: mysql/postgresql/mariadb/mssql/oracle-*
├── test-resources-r2dbc/                 # Aggregator: r2dbc-* (core + db variants + pool)
├── test-resources-hibernate-reactive/    # Aggregator: HR core + db variants
├── test-resources-localstack/            # Aggregator: core + s3/sqs/sns/dynamodb
├── test-resources-mongodb/ neo4j/ kafka/ redis/ rabbitmq/ hivemq/ opensearch/ solr/
└── src/main/docs/                        # Project docs (Micronaut site generator)
```

## WHERE TO LOOK
- Core resolution SPI → test-resources-core/src/main/java/io/micronaut/testresources/core
- Base container providers → test-resources-testcontainers/src/main/java/io/micronaut/testresources/testcontainers
- Remote server entrypoint → test-resources-server/src/main/java/.../TestResourcesService.java
- Client facade → test-resources-client/src/main/java/io/micronaut/testresources/client
- Build helpers (large util) → test-resources-build-tools/src/main/java/.../ServerUtils.java
- JDBC base + per-DB providers → test-resources-jdbc/
- R2DBC base + per-DB providers → test-resources-r2dbc/
- LocalStack shared logic → test-resources-localstack/test-resources-localstack-core
- Build logic (plugins, conventions) → buildSrc/

## CODE MAP
Skipped (LSP not initialized). This is a large, modular Gradle Java/Groovy/Kotlin codebase.

## CONVENTIONS
- Gradle: settings plugin (Micronaut shared), version catalogs; wrapper present.
- Docs: kept under src/main/docs/ (Micronaut site), not ./docs.
- Tests: heavy Spock usage under src/test/groovy and src/spockTest/groovy; Kotlin tests under src/koTest/kotlin.
- Custom source sets observed: src/test2 (embedded); Micronaut AOT under src/aot (server).
- Providers expose properties under clear namespaces (jdbc.*, r2dbc.*, localstack.*) via core resolver SPI.

## ANTI-PATTERNS (THIS PROJECT)
- Do NOT hard-code ports for container providers; prefer ephemeral/random bindings (see Consul note).
- Deprecated Oracle XE variants exist; prefer unified 'oracle' providers (r2dbc/jdbc/hibernate-reactive).
- Avoid blocking operations on Micronaut event loop in server module.
- In Gradle/buildSrc, avoid leaking runtime app deps into build logic; keep tasks cache-friendly and free of network in configuration phase.
- Preserve generated POM metadata markers in build outputs.

## UNIQUE STYLES
- Deep aggregator layout (e.g., test-resources-jdbc/*, r2dbc/*) with shared "core" per technology.
- AOT usage in server; custom source set (test2) in embedded module.
- Workflows synced from template; .github/workflows contains an rsync filter file.

## COMMANDS
```bash
./gradlew build              # Build all modules
./gradlew check              # Run tests
./gradlew publishToMavenLocal
# CI workflows: .github/workflows/*.yml (gradle, release, snapshots, GraalVM variants)
```

## NOTES
- Two complexity hotspots: ServerUtils.java (~700 lines) and a large Spock test in testcontainers.
- Entry point is only in test-resources-server (TestResourcesService).
- Many modules extend shared base providers; start from each technology's "core" submodule before drilling into per-service modules.
