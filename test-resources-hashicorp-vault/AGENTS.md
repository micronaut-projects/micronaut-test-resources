# PROJECT KNOWLEDGE BASE

Generated: 2026-01-20T11:56:32Z
Commit: e716b3e657e0e3b054b70a4920e882446579b8e0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources HashiCorp Vault module: Provides automatic provisioning of HashiCorp Vault test containers for integration tests. Extends core test resources with Vault-specific resolver using Testcontainers.

## STRUCTURE
```
./test-resources-hashicorp-vault/
├── build.gradle                          # Module build configuration
├── src/
│   ├── main/
│   │   ├── java/io/micronaut/testresources/hashicorp/vault/
│   │   │   └── VaultTestResourceProvider.java  # Core resolver implementation
│   │   └── resources/META-INF/services/
│   │       └── io.micronaut.testresources.core.TestResourcesResolver  # Service registration
│   └── test/
│       ├── groovy/io/micronaut/testresources/hashicorp/vault/
│       │   └── VaultStartedTest.groovy    # Spock integration test
│       └── resources/
│           ├── application-test.yml      # Test app config
│           ├── bootstrap.yml             # Bootstrap config
│           └── logback.xml               # Logging config
└── .settings/                            # Eclipse/IDE configs
```

## WHERE TO LOOK
- Core resolver → src/main/java/io/micronaut/testresources/hashicorp/vault/VaultTestResourceProvider.java
- Service registration → src/main/resources/META-INF/services/io.micronaut.testresources.core.TestResourcesResolver
- Integration tests → src/test/groovy/io/micronaut/testresources/hashicorp/vault/VaultStartedTest.groovy
- Build config → build.gradle (depends on testcontainers-vault, micronaut-test-resources-testcontainers)
- Docs snippet → src/main/docs/guide/modules-hashicorp-vault.adoc (config examples)

## CODE MAP
Small module; key class: VaultTestResourceProvider extends AbstractTestContainersProvider<VaultContainer<?>>. Handles container startup, property resolution (vault.client.uri/token), and secret pre-seeding.

## CONVENTIONS
- Use config props like `test-resources.containers.hashicorp-vault.image-name` for image override (default: vault:1.13.3)
- Vault addr/token props: Resolves `vault.client.uri` to container HTTP endpoint; `vault.client.token` defaults to "vault-token"
- Dev server mode: Supports remote test resources server for shared Vault instances; configure via micronaut.test.resources.server.uri
- Pre-seed secrets via config: test-resources.containers.hashicorp-vault.path/secrets
- Follow core test resources SPI for property resolution

## ANTI-PATTERNS (THIS MODULE)
- Committing tokens: Never hardcode or commit real Vault tokens; use test defaults or env vars
- Fixed ports: Avoid hardcoding ports; use Testcontainers' dynamic port mapping
- Direct container management: Don't bypass resolver SPI; extend AbstractTestContainersProvider instead
- Production creds in tests: Separate test Vault config from prod to prevent leaks

## UNIQUE STYLES
- Minimalist module: Single provider class with Spock test; integrates tightly with test-resources-testcontainers base
- Config-driven secrets: Uses YAML lists for startup secret injection

## COMMANDS
```bash
./gradlew :micronaut-test-resources-hashicorp-vault:build    # Build module
./gradlew :micronaut-test-resources-hashicorp-vault:check    # Run tests
./gradlew :micronaut-test-resources-hashicorp-vault:publishToMavenLocal
```

## NOTES
- Depends on Testcontainers Vault module for container orchestration
- Test verifies secret injection (e.g., OAuth configs)
- For broader project context, see root AGENTS.md
