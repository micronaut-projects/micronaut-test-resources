package io.micronaut.testresources.compose

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ComposeTestResourcesResolverTest extends Specification {

    @TempDir
    Path tempDir

    def "resolves PostgreSQL and Redis properties from Compose services"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {
                  "image": "postgres:17",
                  "environment": {
                    "POSTGRES_USER": "demo",
                    "POSTGRES_PASSWORD": "secret",
                    "POSTGRES_DB": "demo"
                  },
                  "labels": {
                    "io.micronaut.test-resources.service": "postgres",
                    "io.micronaut.test-resources.datasource": "default"
                  }
                },
                "cache": {
                  "image": "redis:7",
                  "labels": {
                    "io.micronaut.test-resources.service": "redis"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              },
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)
        def config = config()

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config).get() == "jdbc:postgresql://localhost:15432/demo"
        resolver.resolve("datasources.default.username", ["datasources.default.db-type": "postgres"], config).get() == "demo"
        resolver.resolve("datasources.default.password", ["datasources.default.db-type": "postgres"], config).get() == "secret"
        resolver.resolve("datasources.default.driver-class-name", ["datasources.default.db-type": "postgres"], config).get() == "org.postgresql.Driver"
        resolver.resolve("redis.uri", [:], config).get() == "redis://127.0.0.1:16379"
        cli.commands.count { it.contains("up -d --wait") } == 1
    }

    def "exposes resolver metadata only when Compose configuration is usable"() {
        given:
        createComposeFile()
        def resolver = resolver(new FakeComposeCli(configJson('{"services": {}}'), psJson("[]")))

        expect:
        resolver.name == "compose"
        resolver.displayName == "Docker Compose"
        resolver.order < 0
        resolver.isEnabled(["compose.enabled": true])
        !resolver.isEnabled([:])
        resolver.getRequiredPropertyEntries() == ["datasources", "r2dbc.datasources", "jpa", "mongodb.servers"]
        resolver.getRequiredProperties("datasources.inventory.url") == [
                "datasources.inventory.db-type",
                "datasources.inventory.dialect",
                "datasources.inventory.db-name",
                "datasources.inventory.test-resources.resource-name"
        ]
        resolver.getRequiredProperties("r2dbc.datasources.inventory.url") == [
                "r2dbc.datasources.inventory.db-type",
                "r2dbc.datasources.inventory.dialect",
                "r2dbc.datasources.inventory.driverClassName",
                "r2dbc.datasources.inventory.db-name",
                "r2dbc.datasources.inventory.test-resources.resource-name",
                "datasources.inventory.db-name"
        ]
        resolver.getRequiredProperties("jpa.inventory.properties.hibernate.connection.url") == [
                "jpa.inventory.properties.hibernate.connection.db-type",
                "datasources.inventory.db-type",
                "datasources.inventory.url",
                "datasources.inventory.username",
                "datasources.inventory.password"
        ]
        resolver.getRequiredProperties("redis.uri").empty
        resolver.getRequiredProperties("datasources").empty
        def resolvable = resolver.getResolvableProperties([
                "datasources": ["default", "inventory"],
                "r2dbc.datasources": ["default"],
                "jpa": ["default"],
                "mongodb.servers": ["inventory"]
        ], config())
        resolvable.containsAll([
                "datasources.default.url",
                "datasources.default.username",
                "datasources.default.password",
                "datasources.default.driver-class-name",
                "datasources.inventory.url",
                "datasources.inventory.username",
                "datasources.inventory.password",
                "datasources.inventory.driver-class-name",
                "r2dbc.datasources.default.url",
                "r2dbc.datasources.default.username",
                "r2dbc.datasources.default.password",
                "jpa.default.properties.hibernate.connection.url",
                "jpa.default.properties.hibernate.connection.username",
                "jpa.default.properties.hibernate.connection.password",
                "mongodb.servers.inventory.uri",
                "redis.uri",
                "redis.uris",
                "kafka.bootstrap.servers",
                "mongodb.uri",
                "neo4j.uri",
                "pulsar.service-url",
                "rabbitmq.uri",
                "rabbitmq.username",
                "rabbitmq.password",
                "aws.services.s3.endpoint-override",
                "azure.credential.storage-shared-key.connection-string",
                "couchbase.uri",
                "elasticsearch.http-hosts",
                "hazelcast.client.network.addresses",
                "infinispan.client.hotrod.server.host",
                "micronaut.opensearch.rest-client.http-hosts",
                "minio.url",
                "micronaut.security.oauth2.clients.keycloak.openid.issuer",
                "seaweedfs.url",
                "wiremock.url"
        ])
        resolver.getResolvableProperties(["datasources": ["default"]], [
                "compose.enabled": true,
                "compose.working-directory": Files.createDirectory(tempDir.resolve("empty")).toString()
        ]).empty
    }

    def "does not inspect Compose when disabled or unusable"() {
        given:
        def cli = new FakeComposeCli(configJson('{"services": {}}'), psJson("[]"))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("redis.uri", [:], ["compose.enabled": false]).empty
        resolver.resolve("redis.uri", [:], ["compose.enabled": true, "compose.working-directory": tempDir.toString()]).empty
        cli.commands.empty
    }

    def "returns empty so default providers can handle unmatched services"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {
                  "image": "redis:7",
                  "labels": {
                    "io.micronaut.test-resources.service": "redis"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config()).empty
    }

    def "returns empty when requested datasource type is not PostgreSQL"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "mysql"], config()).empty
        resolver.resolve("datasources.default.url", ["datasources.default.dialect": "MYSQL"], config()).empty
    }

    def "resolves PostgreSQL aliases and label credential overrides"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {
                  "image": "internal/database:latest",
                  "labels": {
                    "io.micronaut.test-resources.service": "postgresql",
                    "io.micronaut.test-resources.datasource": "inventory",
                    "io.micronaut.test-resources.username": "label-user",
                    "io.micronaut.test-resources.password": "label-secret",
                    "io.micronaut.test-resources.database": "label-db"
                  },
                  "environment": {
                    "POSTGRES_USER": "env-user",
                    "POSTGRES_PASSWORD": "env-secret",
                    "POSTGRES_DB": "env-db"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.inventory.url", ["datasources.inventory.db-type": "pg"], config()).get() == "jdbc:postgresql://127.0.0.1:15432/label-db"
        resolver.resolve("datasources.inventory.username", ["datasources.inventory.db-type": "postgresql"], config()).get() == "label-user"
        resolver.resolve("datasources.inventory.password", ["datasources.inventory.dialect": "POSTGRES"], config()).get() == "label-secret"
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config()).empty
    }

    def "ignores explicitly ignored services and unsupported datasource properties"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {
                  "image": "postgres:17",
                  "labels": {
                    "io.micronaut.test-resources.ignore": "true"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config()).empty
        resolver.resolve("datasources.default.schema-generate", ["datasources.default.db-type": "postgres"], config()).empty
        resolver.resolve("datasources.", ["datasources.default.db-type": "postgres"], config()).empty
    }

    def "resolves RabbitMQ properties from Compose service"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "broker": {
                  "image": "rabbitmq:4",
                  "environment": {
                    "RABBITMQ_DEFAULT_USER": "demo",
                    "RABBITMQ_DEFAULT_PASS": "secret"
                  },
                  "labels": {
                    "io.micronaut.test-resources.service": "rabbitmq"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "broker",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5672, "PublishedPort": 15672}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("rabbitmq.uri", [:], config()).get() == "amqp://demo:secret@localhost:15672"
        resolver.resolve("rabbitmq.username", [:], config()).get() == "demo"
        resolver.resolve("rabbitmq.password", [:], config()).get() == "secret"
    }

    def "uses default PostgreSQL and RabbitMQ credentials when Compose omits them"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"},
                "broker": {"image": "rabbitmq:4"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "::", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              },
              {
                "Service": "broker",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5672, "PublishedPort": 15672}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.default.username", [:], config()).get() == "postgres"
        resolver.resolve("datasources.default.password", [:], config()).get() == "postgres"
        resolver.resolve("datasources.default.url", [:], config()).get() == "jdbc:postgresql://localhost:15432/postgres"
        resolver.resolve("rabbitmq.uri", [:], config()).get() == "amqp://guest:guest@localhost:15672"
    }

    def "returns empty for password-protected Redis and unsupported RabbitMQ property"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {
                  "image": "redis:7",
                  "environment": {
                    "REDIS_PASSWORD": "secret"
                  }
                },
                "broker": {"image": "rabbitmq:4"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              },
              {
                "Service": "broker",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 5672, "PublishedPort": 15672}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("redis.uri", [:], config()).empty
        resolver.resolve("rabbitmq.virtual-host", [:], config()).empty
    }

    def "resolves supported database families from Compose services"() {
        given:
        def resolver = singleServiceResolver(
                "database",
                image,
                targetPort,
                publishedPort,
                [:],
                [
                        (ComposeLabels.SERVICE): serviceLabel,
                        (ComposeLabels.DATASOURCE): "inventory"
                ]
        )

        expect:
        resolver.resolve("datasources.inventory.url", [
                "datasources.inventory.db-type": dbType,
                "datasources.inventory.db-name": "inventory"
        ], config()).get() == jdbcUrl
        resolver.resolve("datasources.inventory.username", ["datasources.inventory.db-type": dbType], config()).get() == username
        resolver.resolve("datasources.inventory.password", ["datasources.inventory.db-type": dbType], config()).get() == password
        resolver.resolve("datasources.inventory.driver-class-name", ["datasources.inventory.db-type": dbType], config()).get() == driver
        resolver.resolve("r2dbc.datasources.inventory.url", [
                "r2dbc.datasources.inventory.db-type": dbType,
                "r2dbc.datasources.inventory.db-name": "inventory"
        ], config()).get() == r2dbcUrl
        resolver.resolve("r2dbc.datasources.inventory.username", ["r2dbc.datasources.inventory.db-type": dbType], config()).get() == username
        resolver.resolve("r2dbc.datasources.inventory.password", ["r2dbc.datasources.inventory.db-type": dbType], config()).get() == password
        resolver.resolve("jpa.inventory.properties.hibernate.connection.url", [
                "jpa.inventory.properties.hibernate.connection.db-type": dbType,
                "datasources.inventory.db-name": "inventory"
        ], config()).get() == jdbcUrl
        resolver.resolve("jpa.inventory.properties.hibernate.connection.username", [
                "jpa.inventory.properties.hibernate.connection.db-type": dbType
        ], config()).get() == username
        resolver.resolve("jpa.inventory.properties.hibernate.connection.password", [
                "jpa.inventory.properties.hibernate.connection.db-type": dbType
        ], config()).get() == password

        where:
        image                                        | targetPort | publishedPort | serviceLabel  | dbType      | username | password                      | jdbcUrl                                                         | r2dbcUrl                                      | driver
        "mysql:8"                                    | 3306       | 13306         | "mysql"       | "mysql"     | "test"   | "test"                        | "jdbc:mysql://localhost:13306/inventory"                        | "r2dbc:mysql://localhost:13306/inventory"     | "com.mysql.cj.jdbc.Driver"
        "mariadb:11"                                 | 3306       | 13307         | "maria"       | "mariadb"   | "test"   | "test"                        | "jdbc:mariadb://localhost:13307/inventory"                      | "r2dbc:mariadb://localhost:13307/inventory"   | "org.mariadb.jdbc.Driver"
        "mcr.microsoft.com/mssql/server:2022-latest" | 1433       | 11433         | "sql-server"  | "sqlserver" | "SA"     | "A_Str0ng_Required_Password"  | "jdbc:sqlserver://localhost:11433;databaseName=inventory;encrypt=false" | "r2dbc:mssql://localhost:11433/inventory" | "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        "gvenzl/oracle-free:23"                      | 1521       | 11521         | "oracle-free" | "oracle"    | "test"   | "test"                        | "jdbc:oracle:thin:@localhost:11521/inventory"                   | "r2dbc:oracle://localhost:11521/inventory"    | "oracle.jdbc.OracleDriver"
    }

    def "resolves static service properties from Compose services"() {
        given:
        def resolver = singleServiceResolver(serviceName, image, targetPort, publishedPort, environment, labels)

        expect:
        resolver.resolve(propertyName, [:], config()).get() == expectedValue

        where:
        serviceName | image                                      | targetPort | publishedPort | environment                                      | labels                                                                                 | propertyName                                                         | expectedValue
        "azurite"   | "mcr.microsoft.com/azure-storage/azurite"  | 10000      | 20000         | [:]                                              | [:]                                                                                    | "azure.credential.storage-shared-key.account-name"                   | "devstoreaccount1"
        "couchbase" | "couchbase:community"                      | 11210      | 21210         | [:]                                              | [:]                                                                                    | "couchbase.uri"                                                      | "couchbase://localhost:21210"
        "search"    | "docker.elastic.co/elasticsearch:8"        | 9200       | 19200         | [:]                                              | [:]                                                                                    | "elasticsearch.http-hosts"                                           | "http://localhost:19200"
        "consul"    | "hashicorp/consul:1"                       | 8500       | 18500         | [:]                                              | [:]                                                                                    | "consul.client.default-zone"                                         | "localhost:18500"
        "vault"     | "hashicorp/vault:1"                        | 8200       | 18200         | ["VAULT_DEV_ROOT_TOKEN_ID": "root"]             | [:]                                                                                    | "vault.client.token"                                                 | "root"
        "hazelcast" | "hazelcast/hazelcast:5"                    | 5701       | 15701         | [:]                                              | [:]                                                                                    | "hazelcast.client.network.addresses"                                 | "localhost:15701"
        "mqtt"      | "hivemq/hivemq-ce:latest"                  | 1883       | 11883         | [:]                                              | ["io.micronaut.test-resources.client-id": "mqtt-client"]                              | "mqtt.client.client-id"                                              | "mqtt-client"
        "cachegrid" | "quay.io/infinispan/server:15"             | 11222      | 21222         | ["USER": "admin", "PASS": "secret"]            | [:]                                                                                    | "infinispan.client.hotrod.security.authentication.password"          | "secret"
        "redpanda"  | "redpandadata/redpanda:latest"             | 9092       | 19092         | [:]                                              | [:]                                                                                    | "kafka.bootstrap.servers"                                            | "PLAINTEXT://localhost:19092"
        "aws"       | "localstack/localstack:3"                  | 4566       | 14566         | [:]                                              | [:]                                                                                    | "aws.services.s3.endpoint-override"                                  | "http://localhost:14566"
        "minio"     | "minio/minio:latest"                       | 9000       | 19000         | ["MINIO_ROOT_USER": "access", "MINIO_ROOT_PASSWORD": "secret"] | [:]                                                                           | "minio.secret-key"                                                   | "secret"
        "mongodb"   | "mongo:7"                                  | 27017      | 37017         | ["MONGO_INITDB_DATABASE": "demo"]               | [:]                                                                                    | "mongodb.uri"                                                        | "mongodb://localhost:37017/demo"
        "neo4j"     | "neo4j:5"                                  | 7687       | 17687         | [:]                                              | [:]                                                                                    | "neo4j.uri"                                                          | "bolt://localhost:17687"
        "opensearch"| "opensearchproject/opensearch:2"           | 9200       | 19201         | [:]                                              | [:]                                                                                    | "micronaut.opensearch.httpclient5.http-hosts"                        | "http://localhost:19201"
        "pulsar"    | "apachepulsar/pulsar:3"                    | 6650       | 16650         | [:]                                              | [:]                                                                                    | "pulsar.service-url"                                                 | "pulsar://localhost:16650"
        "seaweedfs" | "chrislusf/seaweedfs:latest"               | 8333       | 18333         | [:]                                              | [:]                                                                                    | "seaweedfs.access-key"                                               | "some_access_key1"
        "solr"      | "solr:9"                                   | 8983       | 18983         | [:]                                              | [:]                                                                                    | "micronaut.solr.hosts"                                               | "http://localhost:18983/solr"
        "zk"        | "zookeeper:3"                              | 9983       | 19983         | [:]                                              | [:]                                                                                    | "micronaut.solr.zk-hosts"                                            | "localhost:19983"
        "keycloak"  | "quay.io/keycloak/keycloak:latest"         | 8080       | 18080         | ["KEYCLOAK_REALM": "demo"]                     | [:]                                                                                    | "micronaut.security.oauth2.clients.keycloak.openid.issuer"           | "http://localhost:18080/realms/demo"
        "wiremock"  | "wiremock/wiremock:3"                      | 8080       | 18081         | [:]                                              | [:]                                                                                    | "wiremock.url"                                                       | "http://localhost:18081"
    }

    def "resolves additional static service property variants from Compose services"() {
        given:
        def resolver = singleServiceResolver(serviceName, image, targetPort, publishedPort, environment, labels)

        expect:
        resolver.resolve(propertyName, [:], config()).get() == expectedValue

        where:
        serviceName | image                                      | targetPort | publishedPort | environment                                      | labels                                                    | propertyName                                                         | expectedValue
        "couchbase" | "couchbase:community"                      | 11210      | 21210         | ["COUCHBASE_USERNAME": "user", "COUCHBASE_PASSWORD": "secret"] | [:]                                           | "couchbase.password"                                                 | "secret"
        "consul"    | "hashicorp/consul:1"                       | 8500       | 18500         | [:]                                              | [:]                                                       | "consul.client.port"                                                 | "18500"
        "vault"     | "hashicorp/vault:1"                        | 8200       | 18200         | [:]                                              | [:]                                                       | "vault.client.uri"                                                   | "http://localhost:18200"
        "mqtt"      | "hivemq/hivemq-ce:latest"                  | 1883       | 11883         | [:]                                              | [:]                                                       | "mqtt.client.server-uri"                                             | "tcp://localhost:11883"
        "cachegrid" | "quay.io/infinispan/server:15"             | 11222      | 21222         | [:]                                              | [:]                                                       | "infinispan.client.hotrod.server.host"                               | "localhost"
        "aws"       | "localstack/localstack:3"                  | 4566       | 14566         | [:]                                              | [:]                                                       | "aws.region"                                                         | "us-east-1"
        "minio"     | "minio/minio:latest"                       | 9000       | 19000         | ["MINIO_ROOT_USER": "access"]                   | [:]                                                       | "minio.access-key"                                                   | "access"
        "opensearch"| "opensearchproject/opensearch:2"           | 9200       | 19201         | [:]                                              | [:]                                                       | "micronaut.opensearch.rest-client.http-hosts"                        | "http://localhost:19201"
        "seaweedfs" | "chrislusf/seaweedfs:latest"               | 8333       | 18333         | [:]                                              | [:]                                                       | "seaweedfs.secret-key"                                               | "some_secret_key1"
        "keycloak"  | "quay.io/keycloak/keycloak:latest"         | 8080       | 18080         | ["KEYCLOAK_REALM": "demo", "KEYCLOAK_CLIENT_SECRET": "secret"] | ["io.micronaut.test-resources.client-id": "client"] | "micronaut.security.token.jwt.signatures.jwks.keycloak.url"          | "http://localhost:18080/realms/demo/protocol/openid-connect/certs"
        "wiremock"  | "wiremock/wiremock:3"                      | 8080       | 18081         | [:]                                              | [:]                                                       | "wiremock.port"                                                      | "18081"
    }

    def "resolves Azurite connection string from all published service ports"() {
        given:
        createComposeFile()
        def resolver = resolver(new FakeComposeCli(configJson("""
            {
              "services": {
                "azurite": {
                  "image": "mcr.microsoft.com/azure-storage/azurite"
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "azurite",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 10000, "PublishedPort": 20000},
                  {"URL": "0.0.0.0", "TargetPort": 10001, "PublishedPort": 20001},
                  {"URL": "0.0.0.0", "TargetPort": 10002, "PublishedPort": 20002}
                ]
              }
            ]
            """)))

        expect:
        resolver.resolve("azure.credential.storage-shared-key.connection-string", [:], config()).get() ==
                "DefaultEndpointsProtocol=http;" +
                "AccountName=devstoreaccount1;" +
                "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
                "BlobEndpoint=http://localhost:20000/devstoreaccount1;" +
                "QueueEndpoint=http://localhost:20001/devstoreaccount1;" +
                "TableEndpoint=http://localhost:20002/devstoreaccount1;"
    }

    def "resolves named MongoDB server properties from Compose services"() {
        given:
        def resolver = singleServiceResolver(
                "documentdb",
                "mongo:7",
                27017,
                37017,
                ["MONGO_INITDB_DATABASE": "inventory"],
                [(ComposeLabels.DATASOURCE): "inventory"]
        )

        expect:
        resolver.resolve("mongodb.servers.inventory.uri", [:], config()).get() == "mongodb://localhost:37017/inventory"
        resolver.resolve("mongodb.servers.inventory.host", [:], config()).empty
        resolver.resolve("mongodb.servers..uri", [:], config()).empty
    }

    def "ambiguous automatic PostgreSQL mapping returns empty"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db1": {"image": "postgres:17"},
                "db2": {"image": "postgres:17"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db1",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              },
              {
                "Service": "db2",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 25432}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config()).empty
    }

    def "falls back when Compose up wait is unsupported"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {"image": "redis:7"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        cli.upWaitResult = new ComposeCommandResult(1, "", "unknown flag: --wait")
        def resolver = resolver(cli)

        expect:
        resolver.resolve("redis.uri", [:], config()).get() == "redis://127.0.0.1:16379"
        cli.commands.contains("up -d --wait")
        cli.commands.contains("up -d")
    }

    def "returns empty when Compose commands fail"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson('{"services": {}}'), psJson("[]"))
        cli.configResult = new ComposeCommandResult(1, "", "invalid compose file")
        def resolver = resolver(cli)

        expect:
        resolver.resolve("redis.uri", [:], config()).empty
    }

    def "returns empty when Compose startup or inspection fails"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {"image": "redis:7"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        cli.upWaitResult = upWaitResult
        cli.afterStartPsResult = afterStartPsResult
        def resolver = resolver(cli)

        expect:
        resolver.resolve("redis.uri", [:], config()).empty

        where:
        upWaitResult                                                | afterStartPsResult
        new ComposeCommandResult(1, "", "container failed to start") | null
        new ComposeCommandResult(0, "", "")                         | new ComposeCommandResult(1, "", "ps failed")
    }

    def "does not stop services that were running before Test Resources started"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              }
            ]
            """))
        cli.beforeStartPs = cli.afterStartPs
        def resolver = resolver(cli)

        when:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config())
        resolver.close()

        then:
        !cli.commands.any { it.startsWith("stop") }
    }

    def "stops only services started and owned by Test Resources"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"},
                "cache": {"image": "redis:7"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              },
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        cli.beforeStartPs = psJson("""
            [
              {
                "Service": "cache",
                "State": "running"
              }
            ]
            """)
        def resolver = resolver(cli)

        when:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config())
        resolver.close()

        then:
        cli.commands.any { it == "stop db" }
        !cli.commands.any { it == "stop cache" }
    }

    def "stop failures for owned services are diagnostic only"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "db",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": 5432, "PublishedPort": 15432}
                ]
              }
            ]
            """))
        cli.stopResult = new ComposeCommandResult(1, "", "stop failed")
        def resolver = resolver(cli)

        when:
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config())
        resolver.close()

        then:
        cli.commands.any { it == "stop db" }
    }

    def "does not start or stop Compose when startup and managed stop are disabled"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {"image": "redis:7"}
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              }
            ]
            """))
        def resolver = resolver(cli)
        def config = config() + ["compose.start": false, "compose.stop-managed": false]

        when:
        resolver.resolve("redis.uri", [:], config)
        resolver.close()

        then:
        !cli.commands.contains("up -d --wait")
        !cli.commands.any { it.startsWith("stop") }
    }

    def "parses collection labels environment and newline-delimited ps output"() {
        given:
        def parser = new ComposeProjectParser()
        def services = parser.parse(configJson("""
            {
              "services": {
                "db": {
                  "image": "postgres:17",
                  "environment": ["POSTGRES_USER=demo", "POSTGRES_PASSWORD=secret"],
                  "labels": ["io.micronaut.test-resources.datasource=inventory"]
                }
              }
            }
            """), psJson("""
            {"Service": "db", "State": "running", "Publishers": [{"URL": "", "TargetPort": "5432", "PublishedPort": "15432"}]}
            {"Service": "cache", "State": "exited"}
            """), ["db"] as Set)

        expect:
        services.size() == 1
        services[0].name() == "db"
        services[0].environment("POSTGRES_USER", "postgres") == "demo"
        services[0].labels()[ComposeLabels.DATASOURCE] == "inventory"
        services[0].publishedPort(5432).get().host() == "localhost"
        services[0].externallyManaged()
        parser.runningServices(psJson("""
            {"Service": "db", "State": "running"}
            {"Service": "cache", "State": "exited"}
            """)) == ["db"] as Set
    }

    def "builds configuration from scalar values"() {
        when:
        def configuration = ComposeConfiguration.from([
                "compose.enabled": "true",
                "compose.working-directory": tempDir.toString(),
                "compose.files": "compose.yml, docker-compose.yml",
                "compose.profiles": "dev,test",
                "compose.start": "false",
                "compose.stop-managed": "false",
                "compose.startup-timeout": "2m"
        ], ["micronaut.test.resources.scope": "test scope"])

        then:
        configuration.enabled()
        configuration.files() == [
                tempDir.resolve("compose.yml").toAbsolutePath().normalize(),
                tempDir.resolve("docker-compose.yml").toAbsolutePath().normalize()
        ]
        configuration.profiles() == ["dev", "test"]
        configuration.projectName().startsWith("mn-tr-")
        !configuration.start()
        !configuration.stopManaged()
        configuration.startupTimeout() == java.time.Duration.ofMinutes(2)
    }

    def "builds default configuration from discovered files and typed values"() {
        given:
        Files.writeString(tempDir.resolve("docker-compose.yaml"), "services: {}\n")

        when:
        def configuration = ComposeConfiguration.from([
                "compose.enabled": true,
                "compose.working-directory": tempDir,
                "compose.profiles": ["dev", "", " test "],
                "compose.startup-timeout": 30
        ])

        then:
        configuration.usable()
        configuration.files() == [tempDir.resolve("docker-compose.yaml").toAbsolutePath().normalize()]
        configuration.profiles() == ["dev", "test"]
        configuration.projectName() == "mn-tr-" + tempDir.fileName.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-")
        configuration.start()
        configuration.stopManaged()
        configuration.startupTimeout() == java.time.Duration.ofSeconds(30)
    }

    def "parses configured startup timeout formats"() {
        expect:
        ComposeConfiguration.from(["compose.startup-timeout": value]).startupTimeout() == expected

        where:
        value                         | expected
        java.time.Duration.ofSeconds(5) | java.time.Duration.ofSeconds(5)
        "1500ms"                      | java.time.Duration.ofMillis(1500)
        "45s"                         | java.time.Duration.ofSeconds(45)
        "PT3S"                        | java.time.Duration.ofSeconds(3)
    }

    def "parses single ps object and ignores invalid config shapes"() {
        given:
        def parser = new ComposeProjectParser()

        expect:
        parser.runningServices(psJson('{"Service": "db", "State": "running"}')) == ["db"] as Set
        parser.parse("[]", "[]", [] as Set).empty
        parser.parse('{"services": []}', "[]", [] as Set).empty
        parser.parse('{"services": {"cache": "redis"}}', "[]", [] as Set).empty
    }

    def "parses empty and partially populated Compose output defensively"() {
        given:
        def parser = new ComposeProjectParser()

        when:
        def services = parser.parse(configJson("""
            {
              "services": {
                "cache": {
                  "image": "redis:7",
                  "labels": {
                    "io.micronaut.test-resources.service": "redis",
                    "ignored-null": null
                  },
                  "environment": ["REDIS_PASSWORD=secret", "MALFORMED"]
                }
              }
            }
            """), psJson("""
            [
              {
                "name": "cache",
                "state": "running",
                "publishers": [
                  {"url": "127.0.0.1", "target_port": "6379", "published_port": "16379"},
                  {"url": "127.0.0.1", "published_port": "26379"}
                ]
              }
            ]
            """), [] as Set)

        then:
        parser.parse("", "", [] as Set).empty
        parser.runningServices("").empty
        services.size() == 1
        services[0].labels() == [(ComposeLabels.SERVICE): "redis"]
        services[0].environment() == ["REDIS_PASSWORD": "secret"]
        services[0].publishedPort(6379).get().publishedPort() == 16379
    }

    def "returns empty for unsupported properties and services without published ports"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {"image": "postgres:17"},
                "cache": {"image": "redis:7"},
                "broker": {"image": "rabbitmq:4"}
              }
            }
            """), psJson("""
            [
              {"Service": "db", "State": "running"},
              {"Service": "cache", "State": "running"},
              {"Service": "broker", "State": "running"}
            ]
            """))
        def resolver = resolver(cli)

        expect:
        resolver.resolve("kafka.bootstrap.servers", [:], config()).empty
        resolver.resolve("datasources.default.url", ["datasources.default.db-type": "postgres"], config()).empty
        resolver.resolve("redis.uri", [:], config()).empty
        resolver.resolve("rabbitmq.uri", [:], config()).empty
    }

    def "reports Compose command diagnostics from stderr or stdout"() {
        expect:
        new ComposeCommandResult(1, "stdout problem\n", "stderr problem\n").diagnostic() == "stderr problem"
        new ComposeCommandResult(1, "stdout problem\n", "").diagnostic() == "stdout problem"
        new ComposeCommandResult(1, null, null).diagnostic() == ""
        !new ComposeCommandResult(1, "", "").successful()
        new ComposeCommandResult(0, "", "").successful()
    }

    def "redacts credential-like diagnostic values"() {
        expect:
        SecretRedactor.redact([
                "POSTGRES_PASSWORD": "secret",
                "RABBITMQ_DEFAULT_USER": "guest",
                "service.api-key": "key"
        ]) == [
                "POSTGRES_PASSWORD": "******",
                "RABBITMQ_DEFAULT_USER": "guest",
                "service.api-key": "******"
        ]
        SecretRedactor.redact("access.token", "secret") == "******"
        SecretRedactor.redact("safe.name", "visible") == "visible"
    }

    private ComposeTestResourcesResolver resolver(FakeComposeCli cli) {
        new ComposeTestResourcesResolver(new ComposeProjectManager(cli, new ComposeProjectParser()), new ComposePropertyMapper())
    }

    private ComposeTestResourcesResolver singleServiceResolver(String serviceName,
                                                               String image,
                                                               int targetPort,
                                                               int publishedPort,
                                                               Map<String, String> environment = [:],
                                                               Map<String, String> labels = [:]) {
        createComposeFile()
        resolver(new FakeComposeCli(configJson("""
            {
              "services": {
                "${serviceName}": {
                  "image": "${image}",
                  "environment": ${json(environment)},
                  "labels": ${json(labels)}
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "${serviceName}",
                "State": "running",
                "Publishers": [
                  {"URL": "0.0.0.0", "TargetPort": ${targetPort}, "PublishedPort": ${publishedPort}}
                ]
              }
            ]
            """)))
    }

    private Map<String, Object> config() {
        [
                "compose.enabled": true,
                "compose.files": ["compose.yml"],
                "compose.working-directory": tempDir.toString(),
                "compose.project-name": "mn-compose-test"
        ]
    }

    private void createComposeFile() {
        Files.writeString(tempDir.resolve("compose.yml"), "services: {}\n")
    }

    private static String configJson(String json) {
        json.stripIndent().trim()
    }

    private static String psJson(String json) {
        json.stripIndent().trim()
    }

    private static String json(Map<String, String> values) {
        "{" + values.collect { entry -> "\"${escapeJson(entry.key)}\":\"${escapeJson(entry.value)}\"" }.join(",") + "}"
    }

    private static String escapeJson(String value) {
        value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private static final class FakeComposeCli implements ComposeCli {
        final String config
        final String afterStartPs
        String beforeStartPs = "[]"
        List<String> commands = []
        ComposeCommandResult upWaitResult = new ComposeCommandResult(0, "", "")
        ComposeCommandResult upResult = new ComposeCommandResult(0, "", "")
        ComposeCommandResult configResult
        ComposeCommandResult afterStartPsResult
        ComposeCommandResult stopResult = new ComposeCommandResult(0, "", "")

        FakeComposeCli(String config, String afterStartPs) {
            this.config = config
            this.afterStartPs = afterStartPs
            this.configResult = new ComposeCommandResult(0, config, "")
        }

        @Override
        ComposeCommandResult run(ComposeConfiguration configuration, List<String> arguments) {
            commands << arguments.join(" ")
            if (arguments == ["ps", "--format", "json"]) {
                if (commands.count { it == "ps --format json" } == 1) {
                    return new ComposeCommandResult(0, beforeStartPs, "")
                }
                return afterStartPsResult ?: new ComposeCommandResult(0, afterStartPs, "")
            }
            if (arguments == ["up", "-d", "--wait"]) {
                return upWaitResult
            }
            if (arguments == ["up", "-d"]) {
                return upResult
            }
            if (arguments == ["config", "--format", "json"]) {
                return configResult
            }
            if (arguments[0] == "stop") {
                return stopResult
            }
            throw new IllegalArgumentException("Unexpected command: $arguments")
        }
    }
}
