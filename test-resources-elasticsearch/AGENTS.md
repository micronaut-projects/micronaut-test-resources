# PROJECT KNOWLEDGE BASE

Generated: 2026-01-22T12:14:22Z
Commit: da5801d0
Branch: kafka-native

## OVERVIEW
Micronaut Test Resources Elasticsearch: Spawns an Elasticsearch testcontainer and resolves elasticsearch.http-hosts.

## STRUCTURE
```
./test-resources-elasticsearch/
├── build.gradle
├── src/main/java/io/micronaut/testresources/elasticsearch/
│   └── ElasticsearchTestResourceProvider.java   # Provider (extends AbstractTestContainersProvider)
├── src/main/resources/META-INF/services/
│   └── io.micronaut.testresources.core.TestResourcesResolver  # SPI registration
├── src/test/groovy/io/micronaut/testresources/elasticsearch/
│   ├── AbstractElasticsearchSpec.groovy
│   └── ElasticsearchStartedTest.groovy
└── src/test/resources/
    └── logback.xml
```

## WHERE TO LOOK
- Provider logic → ElasticsearchTestResourceProvider.java
- Resolved property → elasticsearch.http-hosts ("http://" + container.getHttpHostAddress())
- Image selection → DEFAULT_IMAGE docker.elastic.co/elasticsearch/elasticsearch; if tag "latest" used, provider forces DEFAULT_TAG=8.4.3
- Security → Provider disables xpack security via env var for tests

## CODE MAP
- shouldAnswer only for elasticsearch.http-hosts
- createContainer applies image tag normalization and disables security

## CONVENTIONS
- Namespaces: elasticsearch.* for resolved props; containers.elasticsearch.* for config
- Ports: ephemeral/random; never hard-code
- Respect Testcontainers defaults unless overridden via config

## ANTI-PATTERNS (THIS MODULE)
- Do NOT hard-code ports or credentials
- Do NOT pin "latest" without normalization; provider already guards it
- Avoid blocking operations in providers

## COMMANDS
```bash
./gradlew :micronaut-test-resources-elasticsearch:build
./gradlew :micronaut-test-resources-elasticsearch:check
./gradlew :micronaut-test-resources-elasticsearch:publishToMavenLocal
```

## NOTES
- Tests validate startup and basic HTTP accessibility
- Depends on test-resources-testcontainers and core SPI
