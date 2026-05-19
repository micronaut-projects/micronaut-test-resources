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

        then:
        resolvers.size() == 1
        resolvers[0] instanceof ToggableTestResourcesResolver
        resolvers[0].name == "compose"
        resolvers[0].displayName == "Docker Compose"
        !resolvers[0].isEnabled([:])
        resolvers[0].isEnabled(["compose.enabled": true])
        lifecycles.size() == 1
    }

    def "resolves PostgreSQL datasource and Redis properties from compose metadata"() {
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
        manager.requests == ["db:5432", "cache:6379"]
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

    def "explicit service labels use canonical aliases"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  storage:
    image: custom/internal-azurite
    labels:
      io.micronaut.test-resources.service: azure-storage
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve("azure.credential.storage-shared-key.connection-string", [:], config).get().contains("BlobEndpoint=http://localhost:20000/devstoreaccount1;")
        manager.requests == ["storage:10000"]
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
        manager.requests.empty
    }

    def "resolves database families and non database services from compose metadata"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_USER: app
      MYSQL_PASSWORD: secret
      MYSQL_DATABASE: inventory
    labels:
      io.micronaut.test-resources.service: mysql
  rabbit:
    image: rabbitmq:4
    labels:
      io.micronaut.test-resources.service: rabbitmq
  kafka:
    image: redpandadata/redpanda:v25
    labels:
      io.micronaut.test-resources.service: kafka
  localstack:
    image: localstack/localstack:4
    labels:
      io.micronaut.test-resources.service: localstack
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]
        def props = [
                "datasources.default.db-type": "mysql",
                "r2dbc.datasources.default.db-type": "mysql",
                "jpa.default.properties.hibernate.connection.db-type": "mysql",
        ]

        expect:
        resolver.resolve("datasources.default.url", props, config).get() == "jdbc:mysql://localhost:13306/inventory"
        resolver.resolve("r2dbc.datasources.default.url", props, config).get() == "r2dbc:mysql://localhost:13306/inventory"
        resolver.resolve("jpa.default.properties.hibernate.connection.url", props, config).get() == "jdbc:mysql://localhost:13306/inventory"
        resolver.resolve("rabbitmq.uri", [:], config).get() == "amqp://localhost:15672"
        resolver.resolve("rabbitmq.username", [:], config).get() == "guest"
        resolver.resolve("kafka.bootstrap.servers", [:], config).get() == "localhost:19092"
        resolver.resolve("aws.services.s3.endpoint-override", [:], config).get() == "http://localhost:14566"
        resolver.resolve("aws.region", [:], config).get() == "us-east-1"
    }

    def "resolves additional database descriptor variants"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  db:
    image: ${image}
    labels:
      io.micronaut.test-resources.service: ${service}
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]
        def props = ["datasources.default.db-type": service]

        expect:
        resolver.resolve("datasources.default.url", props, config).get() == expectedUrl
        resolver.resolve("datasources.default.username", props, config).get() == expectedUsername
        resolver.resolve("datasources.default.password", props, config).get() == expectedPassword
        resolver.resolve("datasources.default.driver-class-name", props, config).get() == expectedDriver
        manager.requests == ["db:${port}".toString()]

        where:
        service   | image                 | port | expectedUrl                                               | expectedUsername | expectedPassword              | expectedDriver
        "mariadb" | "mariadb:11"          | 3306 | "jdbc:mariadb://localhost:13306/test"                     | "test"           | "test"                        | "org.mariadb.jdbc.Driver"
        "mssql"   | "mcr.microsoft/mssql" | 1433 | "jdbc:sqlserver://localhost:11433;databaseName=test"      | "SA"             | "A_Str0ng_Required_Password"  | "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        "oracle"  | "gvenzl/oracle-free"  | 1521 | "jdbc:oracle:thin:@//localhost:11521/freepdb1"            | "test"           | "test"                        | "oracle.jdbc.OracleDriver"
    }

    def "resolves supported non database service descriptors"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  service:
    image: internal/${service}
    labels:
      io.micronaut.test-resources.service: ${service}
""".stripIndent())
        def manager = new FakeManager()
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), manager)
        def config = ["compose.enabled": true, "compose.files": [composeFile.toString()]]

        expect:
        resolver.resolve(property, [:], config).get() == expected
        manager.requests == ["service:${port}".toString()]

        where:
        service             | property                                                               | port  | expected
        "azurite"           | "azure.credential.storage-shared-key.account-name"                     | 10000 | "devstoreaccount1"
        "couchbase"         | "couchbase.uri"                                                        | 11210 | "couchbase://localhost:21210"
        "hashicorp-consul"  | "consul.client.port"                                                   | 8500  | "18500"
        "hashicorp-vault"   | "vault.client.uri"                                                     | 8200  | "http://localhost:18200"
        "hazelcast"         | "hazelcast.client.network.addresses"                                   | 5701  | "localhost:15701"
        "hivemq"            | "mqtt.client.server-uri"                                               | 1883  | "tcp://localhost:11883"
        "infinispan"        | "infinispan.client.hotrod.security.authentication.username"            | 11222 | "admin"
        "mailpit"           | "javamail.properties.mail.smtp.auth"                                   | 1025  | "false"
        "minio"             | "minio.url"                                                           | 9000  | "http://localhost:19000"
        "neo4j"             | "neo4j.uri"                                                           | 7687  | "bolt://localhost:17687"
        "keycloak"          | "micronaut.security.oauth2.clients.keycloak.openid.issuer"            | 8080  | "http://localhost:18080/realms/micronaut"
        "opensearch"        | "micronaut.opensearch.rest-client.http-hosts"                         | 9200  | "localhost:19200"
        "opentelemetry"     | "otel.exporter.otlp.endpoint"                                         | 4317  | "http://localhost:14317"
        "pulsar"            | "pulsar.service-url"                                                   | 6650  | "pulsar://localhost:16650"
        "seaweedfs"         | "seaweedfs.access-key"                                                 | 8333  | "some_access_key1"
        "solr"              | "micronaut.solr.hosts"                                                 | 8983  | "http://localhost:18983/solr"
        "wiremock"          | "wiremock.url"                                                         | 8080  | "http://localhost:18080"
    }

    def "parses compose configuration files profiles durations and project names"() {
        given:
        Path composeFile = tempDir.resolve("compose.yaml")
        Files.writeString(composeFile, "services: {}\n")

        when:
        def configuration = ComposeConfiguration.from([
                "compose.enabled": "true",
                "compose.working-directory": tempDir.toString(),
                "compose.files": "compose.yaml",
                "compose.profiles": "dev,test",
                "compose.startup-timeout": "1500ms",
                "compose.local-compose": "true",
                "compose.project-name": "explicit-project"
        ], [:])

        then:
        configuration.usable()
        configuration.files() == [composeFile.toAbsolutePath().normalize()]
        configuration.profiles() == ["dev", "test"]
        configuration.startupTimeout().toMillis() == 1500
        configuration.localCompose()
        configuration.projectName() == "explicit-project"
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
        service.labels()["io.micronaut.test-resources.service"] == "redis"
        service.environment()["REDIS_MODE"] == "standalone"
        service.exposes(6379)
        service.exposes(5432)
        service.exposes(19092)
        service.exposes(11222)
        service.activeFor(["test"])
        !service.activeFor(["dev"])
    }

    def "compose support documents provider coverage and explicit non applicable modules"() {
        expect:
        ComposeServiceDescriptors.allCoveredServiceKinds().containsAll([
                "postgres",
                "mysql",
                "mariadb",
                "mssql",
                "oracle",
                "redis",
                "rabbitmq",
                "kafka",
                "mongodb",
                "localstack",
                "azurite",
                "couchbase",
                "hashicorp-consul",
                "hashicorp-vault",
                "hazelcast",
                "hivemq",
                "infinispan",
                "mailpit",
                "minio",
                "neo4j",
                "keycloak",
                "opensearch",
                "opentelemetry",
                "pulsar",
                "seaweedfs",
                "solr",
                "wiremock",
                "h2",
                "oracle-test-pilot",
                "r2dbc-pool",
                "generic-testcontainers"
        ])
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
