# Micronaut Test Resources Agent Guidance

This repository is the `4.0.x` release-line workspace for Micronaut Test Resources. It is a Gradle multi-module project that provides automatic test resources through resolver SPIs, Testcontainers-backed providers, an embedded mode, a remote server, and build-time client utilities.

This file is intentionally durable: do not add generated timestamps, commit IDs, or branch names from transient analysis runs. Keep root guidance short and move detailed rules into `.agent-instructions/`.

## Load First

- [Architecture map](.agent-instructions/architecture.md) for module layout and ownership boundaries.
- [Build and test](.agent-instructions/build-and-test.md) for Gradle commands, source sets, and verification scope.
- [Documentation](.agent-instructions/docs.md) for guide sources, snippets, release notes, and docs validation.
- [Provider modules](.agent-instructions/provider-modules.md) for resolver and container-provider conventions.
- [Maintenance rules](.agent-instructions/maintenance.md) for recurring project-specific pitfalls.

## Core Rules

- Use the Gradle wrapper from the repository root. Prefer targeted module tasks while developing, then broaden verification when a change affects shared behavior.
- Keep resolver behavior lazy. Core resolver code should describe resolvable and required properties without starting containers or doing unrelated I/O.
- Do not hard-code container or server ports. Use Testcontainers dynamic bindings and Micronaut's random-port configuration.
- Avoid blocking Micronaut event-loop threads in `test-resources-server`; use the blocking executor pattern already present in the module.
- Preserve compatibility modules unless the issue explicitly asks to remove them. When adding new Oracle guidance or examples, prefer the newer Oracle Free/Test Pilot paths over expanding deprecated XE-specific behavior.
- Keep docs in `src/main/docs/guide`; this repository does not use a top-level `docs/` tree.
- Remove stale nested agent guidance instead of letting old generated files override these root instructions.

## Common Commands

```bash
./gradlew check
./gradlew publishGuide
./gradlew docs
./gradlew publishToMavenLocal
```

Run container-backed integration tests only when Docker is available and the task scope warrants it.
