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
    }

    private ComposeTestResourcesResolver resolver(FakeComposeCli cli) {
        new ComposeTestResourcesResolver(new ComposeProjectManager(cli, new ComposeProjectParser()), new ComposePropertyMapper())
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

    private static final class FakeComposeCli implements ComposeCli {
        final String config
        final String afterStartPs
        String beforeStartPs = "[]"
        List<String> commands = []

        FakeComposeCli(String config, String afterStartPs) {
            this.config = config
            this.afterStartPs = afterStartPs
        }

        @Override
        ComposeCommandResult run(ComposeConfiguration configuration, List<String> arguments) {
            commands << arguments.join(" ")
            if (arguments == ["ps", "--format", "json"]) {
                return new ComposeCommandResult(0, commands.count { it == "ps --format json" } == 1 ? beforeStartPs : afterStartPs, "")
            }
            if (arguments == ["up", "-d", "--wait"]) {
                return new ComposeCommandResult(0, "", "")
            }
            if (arguments == ["config", "--format", "json"]) {
                return new ComposeCommandResult(0, config, "")
            }
            if (arguments[0] == "stop") {
                return new ComposeCommandResult(0, "", "")
            }
            throw new IllegalArgumentException("Unexpected command: $arguments")
        }
    }
}
