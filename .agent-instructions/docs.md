# Documentation

The user guide lives under `src/main/docs/guide`, with navigation controlled by `src/main/docs/guide/toc.yml`. Keep `toc.yml` and `.adoc` files in lockstep.

## Local Docs System

- Guide source: `src/main/docs/guide/**/*.adoc`.
- Navigation source of truth: `src/main/docs/guide/toc.yml`.
- Shared images: `src/main/docs/resources/img/`.
- Release notes: `src/main/docs/guide/releaseHistory.adoc` links to GitHub releases rather than maintaining a long local changelog.
- Docs build command: `./gradlew publishGuide` or `./gradlew pG`; output is under `build/docs/index.html`.
- Full docs plus Javadoc command: `./gradlew docs`.

## Authoring Rules

- Use Micronaut docs macros already present in the guide, such as `dependency:` for dependency declarations.
- Prefer runnable examples and source-backed snippets when examples are added.
- Use `[configuration]` blocks for multi-format configuration examples.
- Do not hand-maintain configuration property tables when generated property reference includes are available.
- Keep user-facing documentation focused on what changed, who is affected, how to migrate, and how to verify success.

## When Not To Edit User Docs

Agent guidance, repository maintenance notes, and internal workflow instructions belong in `AGENTS.md` or `.agent-instructions/`, not in the user guide. Only update `src/main/docs/guide` when the user-facing behavior changes.
