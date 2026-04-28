# Maintenance Rules

## High-Risk Areas

- `test-resources-build-tools/src/main/java/io/micronaut/testresources/buildtools/ServerUtils.java` is broad build/runtime glue. Keep changes small and verify callers.
- `test-resources-server` handles HTTP requests, lifecycle expiry, shutdown, and resolver access. Preserve non-blocking request handling.
- Provider families have many sibling modules; shared behavior belongs in the family core module or Testcontainers base module.
- Build logic in `buildSrc` affects every module. Keep it cache-friendly and avoid runtime dependency leakage.

## Project-Specific Pitfalls

- Do not hard-code ports, credentials, or host assumptions for Testcontainers-backed providers.
- Do not add blocking operations to Micronaut event-loop paths.
- Do not preserve generated POM-output markers by hand-editing generated build outputs; change the source build configuration instead.
- Do not add module-local `AGENTS.md` files unless a directory truly needs instructions that differ from the root guidance.
- Keep workflow changes aligned with the upstream template conventions used by `.github/workflows`.

## Instruction Maintenance

This repository previously had many generated `AGENTS.md` files with stale commit and branch metadata. Keep guidance stable and source-oriented. If module-specific guidance is needed, add a focused topic file under `.agent-instructions/` and link it from the root.
