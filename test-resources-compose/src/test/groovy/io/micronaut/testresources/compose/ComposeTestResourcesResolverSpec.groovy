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

    def "service descriptor mappings resolve all property variants"() {
        given:
        def descriptor = ComposeServiceDescriptors.findServiceDescriptor(property).get()
        def service = new ComposeService("service", "internal/${serviceType}".toString(), labels + [(ComposeLabels.SERVICE): serviceType], environment, [descriptor.port()], [])
        def context = new ComposeServiceDescriptors.ResolutionContext(property, new ComposeEndpoint("compose.test", 12345), service)

        expect:
        descriptor.resolve(context) == expected

        where:
        serviceType         | property                                                               | labels                                       | environment                                      | expected
        "rabbitmq"          | "rabbitmq.uri"                                                         | [:]                                          | [:]                                              | "amqp://compose.test:12345"
        "rabbitmq"          | "rabbitmq.username"                                                    | [:]                                          | ["RABBITMQ_DEFAULT_USER": "rabbit"]              | "rabbit"
        "rabbitmq"          | "rabbitmq.password"                                                    | [:]                                          | ["RABBITMQ_DEFAULT_PASS": "secret"]              | "secret"
        "mongodb"           | "mongodb.uri"                                                          | [:]                                          | [:]                                              | "mongodb://compose.test:12345/test"
        "mongodb"           | "mongodb.servers.analytics.uri"                                        | [:]                                          | [:]                                              | "mongodb://compose.test:12345/analytics"
        "localstack"        | "aws.access-key-id"                                                    | [:]                                          | ["AWS_ACCESS_KEY_ID": "access"]                  | "access"
        "localstack"        | "aws.secret-key"                                                       | [:]                                          | ["AWS_SECRET_ACCESS_KEY": "secret"]              | "secret"
        "localstack"        | "aws.region"                                                           | ["io.micronaut.test-resources.region": "eu"] | [:]                                              | "eu"
        "localstack"        | "aws.services.dynamodb.endpoint-override"                              | [:]                                          | [:]                                              | "http://compose.test:12345"
        "azurite"           | "azure.credential.storage-shared-key.account-key"                      | [:]                                          | [:]                                              | "Eby8vdM02xNOcqFeqCnf2A=="
        "azurite"           | "azure.credential.storage-shared-key.connection-string"                | [:]                                          | [:]                                              | "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFeqCnf2A==;BlobEndpoint=http://compose.test:12345/devstoreaccount1;"
        "couchbase"         | "couchbase.username"                                                   | [(ComposeLabels.USERNAME): "admin"]          | [:]                                              | "admin"
        "couchbase"         | "couchbase.password"                                                   | [(ComposeLabels.PASSWORD): "secret"]         | [:]                                              | "secret"
        "hashicorp-consul"  | "consul.client.host"                                                   | [:]                                          | [:]                                              | "compose.test"
        "hashicorp-vault"   | "vault.client.token"                                                   | [:]                                          | ["VAULT_DEV_ROOT_TOKEN_ID": "token"]             | "token"
        "hivemq"            | "mqtt.client.client-id"                                                | [:]                                          | ["MQTT_CLIENT_ID": "client"]                     | "client"
        "infinispan"        | "infinispan.client.hotrod.server.host"                                 | [:]                                          | [:]                                              | "compose.test"
        "infinispan"        | "infinispan.client.hotrod.server.port"                                 | [:]                                          | [:]                                              | "12345"
        "infinispan"        | "infinispan.client.hotrod.security.authentication.password"            | [:]                                          | ["PASS": "secret"]                              | "secret"
        "mailpit"           | "javamail.properties.mail.smtp.host"                                   | [:]                                          | [:]                                              | "compose.test"
        "mailpit"           | "javamail.properties.mail.smtp.port"                                   | [:]                                          | [:]                                              | "12345"
        "mailpit"           | "javamail.properties.mail.smtp.starttls.enable"                        | [:]                                          | [:]                                              | "false"
        "minio"             | "minio.access-key"                                                     | [:]                                          | ["MINIO_ROOT_USER": "minio"]                     | "minio"
        "minio"             | "minio.secret-key"                                                     | [:]                                          | ["MINIO_ROOT_PASSWORD": "secret"]                | "secret"
        "keycloak"          | "micronaut.security.oauth2.clients.keycloak.client-id"                 | [(ComposeLabels.CLIENT_ID): "client"]        | [:]                                              | "client"
        "keycloak"          | "micronaut.security.oauth2.clients.keycloak.client-secret"             | [:]                                          | ["KEYCLOAK_CLIENT_SECRET": "secret"]             | "secret"
        "keycloak"          | "micronaut.security.token.jwt.signatures.jwks.keycloak.url"            | [(ComposeLabels.REALM): "custom"]            | [:]                                              | "http://compose.test:12345/realms/custom/protocol/openid-connect/certs"
        "seaweedfs"         | "seaweedfs.secret-key"                                                 | [:]                                          | ["AWS_SECRET_ACCESS_KEY": "secret"]              | "secret"
        "wiremock"          | "wiremock.host"                                                        | [:]                                          | [:]                                              | "compose.test"
        "wiremock"          | "wiremock.port"                                                        | [:]                                          | [:]                                              | "12345"
    }

    def "database descriptor mappings resolve jdbc r2dbc and hibernate properties"() {
        given:
        def descriptor = ComposeServiceDescriptors.findDatabaseDescriptor(property, properties).get()
        def service = new ComposeService("db", "internal/${serviceType}".toString(), [
                (ComposeLabels.SERVICE): serviceType,
                (ComposeLabels.DATABASE): "orders",
                (ComposeLabels.USERNAME): "user",
                (ComposeLabels.PASSWORD): "secret"
        ], [:], [descriptor.port()], [])
        def endpoint = new ComposeEndpoint("compose.test", 12345)

        expect:
        descriptor.resolve(property, endpoint, service, properties) == expected

        where:
        serviceType | property                                              | properties                                                                | expected
        "postgres"  | "datasources.default.url"                             | ["datasources.default.db-type": "postgres"]                               | "jdbc:postgresql://compose.test:12345/orders"
        "postgres"  | "r2dbc.datasources.default.url"                       | ["r2dbc.datasources.default.db-type": "postgres"]                         | "r2dbc:postgresql://compose.test:12345/orders"
        "postgres"  | "jpa.default.properties.hibernate.connection.url"     | ["jpa.default.properties.hibernate.connection.db-type": "postgres"]       | "jdbc:postgresql://compose.test:12345/orders"
        "postgres"  | "datasources.default.username"                        | ["datasources.default.db-type": "postgres"]                               | "user"
        "postgres"  | "datasources.default.password"                        | ["datasources.default.db-type": "postgres"]                               | "secret"
        "postgres"  | "datasources.default.driver-class-name"               | ["datasources.default.db-type": "postgres"]                               | "org.postgresql.Driver"
        "mysql"     | "datasources.default.url"                             | ["datasources.default.dialect": "mysql"]                                  | "jdbc:mysql://compose.test:12345/orders"
        "mariadb"   | "r2dbc.datasources.default.url"                       | ["r2dbc.datasources.default.driverClassName": "mariadb"]                  | "r2dbc:mariadb://compose.test:12345/orders"
        "mssql"     | "datasources.default.url"                             | ["datasources.default.db-type": "mssql"]                                  | "jdbc:sqlserver://compose.test:12345;databaseName=orders"
        "oracle"    | "jpa.default.properties.hibernate.connection.url"     | ["jpa.default.properties.hibernate.connection.db-type": "oracle"]         | "jdbc:oracle:thin:@//compose.test:12345/orders"
    }

    def "database descriptor returns empty when property is not datasource backed"() {
        expect:
        ComposeServiceDescriptors.findDatabaseDescriptor("server.port", [:]).empty
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

    def "resolver reports required and resolvable property metadata"() {
        given:
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), new FakeManager())

        when:
        def resolvable = resolver.getResolvableProperties([
                "datasources": ["default"],
                "r2dbc.datasources": ["reactive"],
                "jpa": ["inventory"],
                "mongodb.servers": ["analytics"]
        ], [:])

        then:
        resolver.order == -10
        resolver.requiredPropertyEntries == ["datasources", "r2dbc.datasources", "jpa", "mongodb.servers"]
        resolvable.containsAll([
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.driver-class-name",
                "r2dbc.datasources.reactive.url",
                "r2dbc.datasources.reactive.username",
                "r2dbc.datasources.reactive.password",
                "jpa.inventory.properties.hibernate.connection.url",
                "jpa.inventory.properties.hibernate.connection.username",
                "jpa.inventory.properties.hibernate.connection.password",
                "mongodb.servers.analytics.uri",
                "redis.uri",
                "rabbitmq.uri"
        ])
    }

    def "resolver reports required datasource properties for datasource families"() {
        given:
        def resolver = new ComposeTestResourcesResolver(new ComposeMetadataParser(), new FakeManager())

        expect:
        resolver.getRequiredProperties("server.port").empty
        resolver.getRequiredProperties("datasources.default.url").containsAll([
                "datasources.default.db-type",
                "datasources.default.dialect",
                "datasources.default.db-name",
                "datasources.default.test-resources.resource-name"
        ])
        resolver.getRequiredProperties("r2dbc.datasources.default.url").containsAll([
                "datasources.default.url",
                "r2dbc.datasources.default.db-type",
                "r2dbc.datasources.default.dialect",
                "r2dbc.datasources.default.driverClassName",
                "r2dbc.datasources.default.db-name",
                "r2dbc.datasources.default.test-resources.resource-name",
                "datasources.default.db-name"
        ])
        resolver.getRequiredProperties("jpa.default.properties.hibernate.connection.url").containsAll([
                "jpa.default.properties.hibernate.connection.db-type",
                "datasources.default.db-type",
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.db-name"
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

    def "container manager returns empty for unusable configuration"() {
        given:
        def configuration = ComposeConfiguration.from(["compose.enabled": true], [:])
        def project = new ComposeProject([new ComposeService("cache", "redis:7", [:], [:], [6379], [])])

        expect:
        new ComposeContainerManager().endpoint(configuration, project, project.services().first(), 6379, [:]).empty
    }

    def "container manager close operations are idempotent without running environments"() {
        expect:
        !ComposeContainerManager.closeScope("test")
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

    def "parses compose list labels environments scalar profiles and invalid ports"() {
        given:
        Path composeFile = tempDir.resolve("compose.yml")
        Files.writeString(composeFile, """
services:
  mapped:
    image: redis:7
    labels:
      - io.micronaut.test-resources.service=redis
    environment:
      - REDIS_MODE=standalone
      - IGNORED
    ports:
      - invalid
      - "127.0.0.1:16379:6379/tcp"
    profiles: test
""".stripIndent())

        when:
        def project = new ComposeMetadataParser().parse(ComposeConfiguration.from([
                "compose.enabled": true,
                "compose.files": [composeFile.toString()]
        ], [:]))
        def service = project.services().first()

        then:
        service.labels()[ComposeLabels.SERVICE] == "redis"
        service.environment()["REDIS_MODE"] == "standalone"
        service.exposes(6379)
        !service.exposes(1)
        service.activeFor(["test"])
        !service.activeFor(["dev"])
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
