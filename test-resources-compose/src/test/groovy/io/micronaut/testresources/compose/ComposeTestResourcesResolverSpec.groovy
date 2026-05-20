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

import java.nio.file.FileSystems
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

    def "compose coverage inventory covers every resolver backed provider module"() {
        given:
        Path root = repositoryRoot()
        def resolverModules = modulesWithService(root, "io.micronaut.testresources.core.TestResourcesResolver") -
                ["test-resources-compose"]
        def composeProviderModules = modulesWithService(root, "io.micronaut.testresources.compose.ComposeTestResourcesProvider")
        def coveredModules = COMPOSE_SUPPORTED_PROVIDER_MODULES + NON_COMPOSE_PROVIDER_RATIONALE.keySet()

        expect:
        resolverModules == coveredModules
        composeProviderModules == COMPOSE_SUPPORTED_PROVIDER_MODULES
        NON_COMPOSE_PROVIDER_RATIONALE.every { module, reason ->
            module && reason.trim()
        }
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
        !ComposeConfiguration.from(["compose.enabled": "true"], [:]).usable()
        ComposeConfiguration.from(["compose.enabled": "true"], [:]).enabled()
        ComposeConfiguration.from(["compose.enabled": false], [:]).dockerImageName() == "docker"
        ComposeConfiguration.from(["compose.startup-timeout": "2s"], [:]).startupTimeout().seconds == 2
        ComposeConfiguration.from(["compose.startup-timeout": "250ms"], [:]).startupTimeout().toMillis() == 250
        ComposeConfiguration.from(["compose.startup-timeout": "3m"], [:]).startupTimeout().seconds == 180
        ComposeConfiguration.from(["compose.startup-timeout": "PT4S"], [:]).startupTimeout().seconds == 4
        ComposeConfiguration.from(["compose.startup-timeout": 5], [:]).startupTimeout().seconds == 5
        ComposeConfiguration.from(["compose.docker-image-name": "docker:27"], [:]).dockerImageName() == "docker:27"
        ComposeConfiguration.from(["compose.docker-image-name": " "], [:]).dockerImageName() == "docker"
        ComposeConfiguration.from(["compose.local-compose": "true"], [:]).localCompose()
        ComposeConfiguration.from(["compose.project-name": "compose-project"], [:]).projectName() == "compose-project"
        ComposeConfiguration.from([:], ["micronaut.test.resources.scope": " "]).projectName().startsWith("mn-tr-")
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

    def "database compose providers expose JDBC R2DBC and Hibernate Reactive metadata"() {
        given:
        def jdbc = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.JDBC, "postgres", "postgresql", "postgresql")
        def r2dbc = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.R2DBC, "postgres", "postgresql", "postgresql")
        def hibernate = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.HIBERNATE_REACTIVE, "postgres", "postgresql", "postgresql")

        expect:
        jdbc.requiredPropertyEntries == ["datasources"]
        jdbc.getResolvableProperties(["datasources": ["default"]]).containsAll([
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.driver-class-name"
        ])
        jdbc.getRequiredProperties("datasources.default.url").containsAll([
                "datasources.default.db-type",
                "datasources.default.dialect",
                "datasources.default.db-name",
                "datasources.default.test-resources.resource-name"
        ])

        r2dbc.requiredPropertyEntries == ["datasources", "r2dbc.datasources"]
        r2dbc.getResolvableProperties([
                "r2dbc.datasources": ["default", "analytics"],
                "datasources": ["default"]
        ]).count { it.endsWith(".url") } == 2
        r2dbc.getRequiredProperties("r2dbc.datasources.default.url").containsAll([
                "datasources.default.url",
                "r2dbc.datasources.default.db-type",
                "r2dbc.datasources.default.dialect",
                "r2dbc.datasources.default.driverClassName",
                "r2dbc.datasources.default.db-name",
                "r2dbc.datasources.default.test-resources.resource-name",
                "datasources.default.db-name"
        ])

        hibernate.requiredPropertyEntries == ["datasources", "jpa"]
        hibernate.getResolvableProperties([
                "jpa": ["default"],
                "datasources": ["default"]
        ]) == [
                "jpa.default.properties.hibernate.connection.url",
                "jpa.default.properties.hibernate.connection.username",
                "jpa.default.properties.hibernate.connection.password"
        ]
        hibernate.getRequiredProperties("jpa.default.properties.hibernate.connection.url").containsAll([
                "jpa.default.properties.hibernate.connection.db-type",
                "datasources.default.db-type",
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.db-name"
        ])
    }

    def "database compose providers match requested datasource and configured database type"() {
        given:
        def provider = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.JDBC, "postgres", "postgresql", "postgresql")
        def defaultService = new ComposeService("db", "custom/image", [
                (ComposeLabels.SERVICE): "postgres"
        ], [:], [5432], [])
        def inventoryService = new ComposeService("inventory", "custom/image", [
                (ComposeLabels.SERVICE): "postgres",
                (ComposeLabels.DATASOURCE): "inventory"
        ], [:], [5432], [])

        expect:
        provider.matches("datasources.default.url", defaultService)
        provider.matches("datasources.inventory.url", inventoryService)
        !provider.matches("datasources.analytics.url", inventoryService)
        !provider.matches("redis.uri", defaultService)
        provider.supports("datasources.default.url", ["datasources.default.db-type": "postgres"])
        provider.supports("datasources.default.url", ["datasources.default.dialect": "PostgreSQL"])
        provider.supports("r2dbc.datasources.default.url", ["r2dbc.datasources.default.driverClassName": "io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider"])
        provider.supports("jpa.default.properties.hibernate.connection.url", ["jpa.default.properties.hibernate.connection.db-type": "postgres"])
        !provider.supports("datasources.default.url", ["datasources.default.db-type": "mysql"])
        !provider.supports("redis.uri", [:])
    }

    def "database compose providers resolve URLs and credentials from labels environment and properties"() {
        given:
        def postgres = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.JDBC, "postgres", "postgresql", "postgresql")
        def mssql = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.JDBC, "mssql", "sqlserver", "mssql")
        def oracle = databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind.R2DBC, "oracle", "oracle:thin", "oracle")
        def service = new ComposeService("db", "postgres:17", [
                (ComposeLabels.USERNAME): "label-user"
        ], [
                "POSTGRES_PASSWORD": "env-secret",
                "POSTGRES_DB": "envdb"
        ], [5432], [])

        expect:
        postgres.resolve(context("datasources.default.url", 5432, service, [:])) == "jdbc:postgresql://localhost:15432/envdb"
        postgres.resolve(context("datasources.default.username", 5432, service, [:])) == "label-user"
        postgres.resolve(context("datasources.default.password", 5432, service, [:])) == "env-secret"
        postgres.resolve(context("datasources.default.driver-class-name", 5432, service, [:])) == "driver.Postgres"
        postgres.resolve(context("datasources.default.db-name", 5432, service, [:])) == null
        postgres.resolve(context("datasources.inventory.url", 5432, service, ["datasources.inventory.db-name": "configured"])) == "jdbc:postgresql://localhost:15432/configured"
        mssql.resolve(context("datasources.default.url", 1433, service, [:])) == "jdbc:sqlserver://localhost:11433;databaseName=envdb"
        oracle.resolve(context("r2dbc.datasources.default.url", 1521, service, [:])) == "r2dbc:oracle://localhost:11521/envdb"
    }

    private static ComposeTestResourcesProvider.ResolutionContext context(String propertyName,
                                                                          int port,
                                                                          ComposeService service,
                                                                          Map<String, Object> properties) {
        new ComposeTestResourcesProvider.ResolutionContext(propertyName, new ComposeEndpoint("localhost", port + 10000), service, properties)
    }

    private static AbstractComposeDatabaseTestResourcesProvider databaseProvider(AbstractComposeDatabaseTestResourcesProvider.Kind kind,
                                                                                String serviceType,
                                                                                String jdbcScheme,
                                                                                String r2dbcScheme) {
        new TestDatabaseComposeTestResourcesProvider(kind, new AbstractComposeDatabaseTestResourcesProvider.Metadata(
                serviceType,
                [serviceType + "-alias", r2dbcScheme] as Set,
                5432,
                "jdbc:" + jdbcScheme,
                r2dbcScheme,
                "driver." + serviceType.capitalize(),
                "POSTGRES_USER",
                "POSTGRES_PASSWORD",
                "POSTGRES_DB",
                "user",
                "password",
                "db"
        ))
    }

    private static Set<String> modulesWithService(Path root, String serviceName) {
        def suffix = "src/main/resources/META-INF/services/$serviceName"
        def modules = [] as Set
        Files.walk(root).withCloseable { stream ->
            stream.filter { Files.isRegularFile(it) }
                    .map { root.relativize(it).toString().replace(FileSystems.default.separator, "/") }
                    .filter { it.endsWith(suffix) }
                    .forEach { modules << it.substring(0, it.length() - suffix.length() - 1) }
        }
        modules
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath()
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.parent
        }
        current
    }

    private static final Set<String> COMPOSE_SUPPORTED_PROVIDER_MODULES = [
            "test-resources-jdbc/test-resources-jdbc-postgresql",
            "test-resources-kafka",
            "test-resources-localstack/test-resources-localstack-core",
            "test-resources-mongodb",
            "test-resources-rabbitmq",
            "test-resources-redis"
    ] as Set

    private static final Map<String, String> NON_COMPOSE_PROVIDER_RATIONALE = [
            "test-resources-azure":
                    "Azurite mapping is not implemented in this revision; normal provider fallback remains.",
            "test-resources-couchbase":
                    "Couchbase bucket/user/bootstrap mapping is not implemented in this revision.",
            "test-resources-elasticsearch":
                    "Module is disabled in settings.gradle and not part of regular build scope.",
            "test-resources-hashicorp-consul":
                    "Consul token and endpoint mapping is not implemented in this revision.",
            "test-resources-hashicorp-vault":
                    "Vault token and endpoint mapping is not implemented in this revision.",
            "test-resources-hazelcast":
                    "Hazelcast cluster/member mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-mariadb":
                    "Hibernate Reactive MariaDB Compose mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-mssql":
                    "Hibernate Reactive MSSQL Compose mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-mysql":
                    "Hibernate Reactive MySQL Compose mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-oracle-free":
                    "Hibernate Reactive Oracle Free Compose mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-oracle-xe":
                    "Hibernate Reactive Oracle XE Compose mapping is not implemented in this revision.",
            "test-resources-hibernate-reactive/test-resources-hibernate-reactive-postgresql":
                    "Hibernate Reactive PostgreSQL Compose mapping is not implemented in this revision.",
            "test-resources-hivemq":
                    "MQTT broker mapping is not implemented in this revision.",
            "test-resources-infinispan":
                    "Infinispan endpoint and credential mapping is not implemented in this revision.",
            "test-resources-jdbc/test-resources-jdbc-h2":
                    "H2 is an in-process database provider and has no reusable Compose service.",
            "test-resources-jdbc/test-resources-jdbc-mariadb":
                    "JDBC MariaDB Compose mapping is not implemented in this revision.",
            "test-resources-jdbc/test-resources-jdbc-mssql":
                    "JDBC MSSQL Compose mapping is not implemented in this revision.",
            "test-resources-jdbc/test-resources-jdbc-mysql":
                    "JDBC MySQL Compose mapping is not implemented in this revision.",
            "test-resources-jdbc/test-resources-jdbc-oracle-free":
                    "JDBC Oracle Free Compose mapping is not implemented in this revision.",
            "test-resources-jdbc/test-resources-jdbc-oracle-test-pilot":
                    "Oracle Test Pilot has no reusable Compose service contract in this revision.",
            "test-resources-jdbc/test-resources-jdbc-oracle-xe":
                    "JDBC Oracle XE Compose mapping is not implemented in this revision.",
            "test-resources-mailpit":
                    "Mailpit SMTP/API property mapping is not implemented in this revision.",
            "test-resources-minio":
                    "MinIO endpoint and credential mapping is not implemented in this revision.",
            "test-resources-neo4j":
                    "Neo4j URI and credential mapping is not implemented in this revision.",
            "test-resources-oauth2":
                    "Keycloak realm/client mapping is not implemented in this revision.",
            "test-resources-opensearch":
                    "OpenSearch endpoint and credential mapping is not implemented in this revision.",
            "test-resources-opentelemetry":
                    "OpenTelemetry collector endpoint mapping is not implemented in this revision.",
            "test-resources-pulsar":
                    "Pulsar broker/admin endpoint mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-mariadb":
                    "R2DBC MariaDB Compose mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-mssql":
                    "R2DBC MSSQL Compose mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-mysql":
                    "R2DBC MySQL Compose mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-oracle-free":
                    "R2DBC Oracle Free Compose mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-oracle-xe":
                    "R2DBC Oracle XE Compose mapping is not implemented in this revision.",
            "test-resources-r2dbc/test-resources-r2dbc-pool":
                    "R2DBC pool is a wrapper provider and has no independent Compose service.",
            "test-resources-r2dbc/test-resources-r2dbc-postgresql":
                    "R2DBC PostgreSQL Compose mapping is not implemented in this revision.",
            "test-resources-seaweedfs":
                    "SeaweedFS S3 endpoint and credential mapping is not implemented in this revision.",
            "test-resources-solr":
                    "Solr endpoint mapping is not implemented in this revision.",
            "test-resources-testcontainers":
                    "Generic Testcontainers has no stable provider-specific property contract.",
            "test-resources-wiremock":
                    "WireMock endpoint mapping is not implemented in this revision."
    ]

    private static final class TestDatabaseComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
        TestDatabaseComposeTestResourcesProvider(Kind kind, Metadata metadata) {
            super(kind, metadata)
        }
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
