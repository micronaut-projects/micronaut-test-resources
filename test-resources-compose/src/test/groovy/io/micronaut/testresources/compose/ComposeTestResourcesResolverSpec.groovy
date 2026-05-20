/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.compose

import io.micronaut.testresources.core.ScopedTestResourcesLifecycle
import io.micronaut.testresources.core.TestResourcesResolver
import io.micronaut.testresources.core.ToggableTestResourcesResolver
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ComposeTestResourcesResolverSpec extends Specification {

    @TempDir
    Path tempDir

    def "compose resolver is opt-in and advertised by service loader"() {
        when:
        def resolvers = ServiceLoader.load(TestResourcesResolver, getClass().classLoader).findAll {
            it instanceof ComposeTestResourcesResolver
        }
        def lifecycles = ServiceLoader.load(ScopedTestResourcesLifecycle, getClass().classLoader).findAll {
            it instanceof ComposeTestResourcesLifecycle
        }
        def providers = ServiceLoader.load(ComposeTestResourcesProvider, getClass().classLoader).toList()

        then:
        resolvers.size() == 1
        resolvers[0] instanceof ToggableTestResourcesResolver
        resolvers[0].name == "compose"
        resolvers[0].displayName == "Docker Compose"
        !resolvers[0].isEnabled([:])
        resolvers[0].isEnabled(["compose.enabled": true])
        lifecycles.size() == 1
        providers*.serviceType.containsAll(["postgres", "redis", "rabbitmq", "kafka", "mongodb", "localstack"])
    }

    def "redacts compose diagnostic credential keys"() {
        expect:
        SecretRedactor.redact([(key): "secret"])[key] == "****"

        where:
        key << [
                "PASS",
                "io.micronaut.test-resources.access-key",
                "azure.credential.storage-shared-key.account-key",
                "AWS_ACCESS_KEY_ID",
                "datasources.default.password",
                "vault.client.token"
        ]
    }

    def "resolves PostgreSQL datasource and Redis properties from provider modules"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  db:
    image: postgres:17
    environment:
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: demo
    labels:
      io.micronaut.test-resources.service: postgres
      io.micronaut.test-resources.datasource: default
  cache:
    image: redis:7
    labels:
      io.micronaut.test-resources.service: redis
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]
        def props = ["datasources.default.db-type": "postgres"]

        expect:
        resolver.resolve("datasources.default.url", props, config).get() == "jdbc:postgresql://localhost:15432/demo"
        resolver.resolve("datasources.default.username", props, config).get() == "demo"
        resolver.resolve("datasources.default.password", props, config).get() == "secret"
        resolver.resolve("datasources.default.driver-class-name", props, config).get() == "org.postgresql.Driver"
        resolver.resolve("redis.uri", [:], config).get() == "redis://localhost:16379"
        manager.requests == ["db:5432", "db:5432", "db:5432", "db:5432", "cache:6379"]
    }

    def "returns empty for ambiguous postgres match so normal provider can fall back"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  first:
    image: postgres:17
  second:
    image: postgres:17
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config).empty
        manager.requests.empty
    }

    def "profile filtering ignores inactive services"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  inactive:
    image: redis:7
    profiles: [extra]
  active:
    image: redis:7
    profiles: [test]
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)

        expect:
        resolver.resolve("redis.uri", [:], ["compose.enabled": true, "compose.files": [composeFile.toString()], "compose.profiles": ["test"]]).get() == "redis://localhost:16379"
        manager.requests == ["active:6379"]
    }

    def "explicit service label prevents same-port heuristic match"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  broker:
    image: rabbitmq:4
    ports:
      - "9092"
    labels:
      io.micronaut.test-resources.service: rabbitmq
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve("kafka.bootstrap.servers", [:], config).empty
        resolver.resolve("rabbitmq.uri", [:], config).get() == "amqp://localhost:15672"
        manager.requests == ["broker:5672"]
    }

    def "authenticated redis is ignored for initial support"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  cache:
    image: redis:7
    environment:
      REDIS_PASSWORD: secret
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)

        expect:
        resolver.resolve("redis.uri", [:], ["compose.enabled": true, "compose.files": [composeFile.toString()]]).empty
        manager.requests == ["cache:6379"]
    }

    def "resolves provider owned non database services from compose metadata"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  rabbit:
    image: rabbitmq:4
    labels:
      io.micronaut.test-resources.service: rabbitmq
  kafka:
    image: redpandadata/redpanda:v25
    labels:
      io.micronaut.test-resources.service: kafka
  mongo:
    image: mongo:8
    labels:
      io.micronaut.test-resources.service: mongodb
  localstack:
    image: localstack/localstack:4
    labels:
      io.micronaut.test-resources.service: localstack
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve("rabbitmq.uri", [:], config).get() == "amqp://localhost:15672"
        resolver.resolve("rabbitmq.username", [:], config).get() == "guest"
        resolver.resolve("kafka.bootstrap.servers", [:], config).get() == "localhost:19092"
        resolver.resolve("mongodb.uri", [:], config).get() == "mongodb://localhost:37017/test"
        resolver.resolve("mongodb.servers.analytics.uri", [:], config).get() == "mongodb://localhost:37017/analytics"
        resolver.resolve("aws.services.s3.endpoint-override", [:], config).get() == "http://localhost:14566"
        resolver.resolve("aws.region", [:], config).get() == "us-east-1"
    }

    def "resolver reports required and resolvable property metadata from providers"() {
        given:
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), new FakeManager())

        when:
        def resolvable = resolver.getResolvableProperties([
                "datasources": ["default"],
                "mongodb.servers": ["analytics"]
        ], [:])

        then:
        resolver.order == -10
        resolver.requiredPropertyEntries.containsAll(["datasources", "mongodb.servers"])
        resolvable.containsAll([
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.driver-class-name",
                "mongodb.servers.analytics.uri",
                "redis.uri",
                "rabbitmq.uri",
                "kafka.bootstrap.servers",
                "aws.services.s3.endpoint-override"
        ])
    }

    def "disabled missing or unreadable compose configuration falls back without endpoint lookup"() {
        given:
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)

        expect:
        resolver.resolve("redis.uri", [:], ["compose.files": [tempDir.resolve("compose.yml").toString()]]).empty
        resolver.resolve("redis.uri", [:], ["compose.enabled": true, "compose.working-directory": tempDir.toString()]).empty
        resolver.resolve("redis.uri", [:], ["compose.enabled": true, "compose.files": [tempDir.resolve("missing.yml").toString()]]).empty
        manager.requests.empty
    }

    def "parses compose metadata maps lists exposed ports and inactive profiles"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  mapped:
    image: redis:7
    labels:
      io.micronaut.test-resources.service: redis
    environment:
      REDIS_MODE: standalone
    ports:
      - target: 6379
      - "127.0.0.1:15432:5432/tcp"
      - "19092-19093"
    expose:
      - "11222"
    profiles:
      - test
""".stripIndent())

        when:
        def project = new ComposeMetadataParser().parse(ComposeConfiguration.from([
                "compose.enabled": true,
                "compose.files": [composeFile.toString()]
        ], [:]))
        def service = project.services().first()

        then:
        service.name() == "mapped"
        service.imageContains("redis")
        service.labels()[ComposeLabels.SERVICE] == "redis"
        service.environment()["REDIS_MODE"] == "standalone"
        service.exposes(6379)
        service.exposes(5432)
        service.exposes(19092)
        service.exposes(11222)
        service.activeFor(["test"])
        !service.activeFor(["dev"])
    }

    def "container manager returns empty for unusable configuration"() {
        given:
        def configuration = ComposeConfiguration.from(["compose.enabled": true], [:])
        def project = new ComposeProject([new ComposeService("cache", "redis:7", [:], [:], [6379], [])])

        expect:
        new ComposeContainerManager().endpoint(configuration, project, project.services().first(), 6379, [:]).empty
    }

    def "lifecycle close operations are noops when no compose environment was started"() {
        given:
        def lifecycle = new ComposeTestResourcesLifecycle()

        expect:
        !lifecycle.closeScope("scope")
        !lifecycle.closeAll()
        !ComposeContainerManager.closeScope("scope")
        !ComposeContainerManager.closeAll()
    }

    def "configuration discovers default files and derives scoped project names"() {
        given:
        Files.writeString(tempDir.resolve("docker-compose.yaml"), "services: {}\n")

        when:
        def configuration = ComposeConfiguration.from([
                "compose.enabled": true,
                "compose.working-directory": tempDir.toString(),
                "compose.startup-timeout": 2,
                "compose.profiles": ["dev", " ", "test"]
        ], [
                "micronaut.test.resources.scope": "Feature/One"
        ])

        then:
        configuration.usable()
        configuration.files() == [tempDir.resolve("docker-compose.yaml").toAbsolutePath().normalize()]
        configuration.profiles() == ["dev", "test"]
        configuration.startupTimeout().seconds == 2
        configuration.dockerImageName() == "docker"
        configuration.projectName().startsWith("mn-tr-")
        configuration.projectName().contains("-feature-one")
    }

    def "configuration parses duration suffixes and explicit docker image"() {
        expect:
        ComposeConfiguration.from(["compose.startup-timeout": "2s"], [:]).startupTimeout().seconds == 2
        ComposeConfiguration.from(["compose.startup-timeout": "3m"], [:]).startupTimeout().seconds == 180
        ComposeConfiguration.from(["compose.startup-timeout": "PT4S"], [:]).startupTimeout().seconds == 4
        ComposeConfiguration.from(["compose.docker-image-name": "docker:27"], [:]).dockerImageName() == "docker:27"
    }

    def "resolves explicitly labelled non default datasource"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  defaultdb:
    image: postgres:17
    labels:
      io.micronaut.test-resources.service: postgres
      io.micronaut.test-resources.datasource: default
  inventorydb:
    image: postgres:17
    environment:
      POSTGRES_DB: inventory
    labels:
      io.micronaut.test-resources.service: postgres
      io.micronaut.test-resources.datasource: inventory
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve("datasources.inventory.url", ["datasources.inventory.db-type": "postgres"], config).get() == "jdbc:postgresql://localhost:15432/inventory"
        manager.requests == ["inventorydb:5432"]
    }

    private static final class FakeManager implements ComposeEnvironmentManager {
        final List<String> requests = []

        @Override
        Optional<ComposeEndpoint> endpoint(ComposeConfiguration configuration,
                                          ComposeProject project,
                                          ComposeService service,
                                          int port,
                                          Map<String, Object> properties) {
            requests << "${service.name()}:${port}".toString()
            Optional.of(new ComposeEndpoint("localhost", port + 10000))
        }
    }
}
