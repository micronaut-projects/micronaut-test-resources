# test-resources-bom KNOWLEDGE BASE

Generated: 2026-01-20T00:00:00Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
BOM module providing dependency management for Micronaut Test Resources modules via Gradle BOM plugin.

## STRUCTURE
```
./test-resources-bom/
├── build.gradle.kts                    # BOM plugin configuration
└── build/publications/maven/           # Generated BOM POM
```

## WHERE TO LOOK
- BOM configuration → build.gradle.kts
- Generated POM → build/publications/maven/pom-default.xml
- Version management → ../gradle/libs.versions.toml
- Release notes → ../MAINTAINING.md (bomProperty references)

## CODE MAP
No source code; BOM defined purely through build configuration.
- **BOM Plugin**: io.micronaut.build.internal.bom generates platform POM.
- **Binary Compatibility**: Configured for post-3.0.0 enforcement.

## CONVENTIONS
- Version alignment via gradle/libs.versions.toml catalogs.
- Binary compatibility enabled after 3.0.0 milestone.
- No runtime dependencies; BOM-only publication.

## ANTI-PATTERNS (THIS MODULE)
- Do not add module dependencies to BOM; restrict to external deps.
- Avoid hardcoding versions; use catalogs/properties.
- Keep build.gradle.kts minimal; delegate to plugins.

## UNIQUE STYLES
- Plugin-driven: Relies on io.micronaut.build.internal.bom for generation.
- No source code: Pure build artifact.

## COMMANDS
```bash
../gradlew :micronaut-test-resources-bom:build     # Generate BOM
../gradlew :micronaut-test-resources-bom:publishToMavenLocal  # Publish locally
```

## NOTES
- Enables consistent versioning across test-resources modules.
- Imported by consumers as platform(mnTest.micronaut.testresources.bom).
- Foundation for multi-module dependency alignment.
