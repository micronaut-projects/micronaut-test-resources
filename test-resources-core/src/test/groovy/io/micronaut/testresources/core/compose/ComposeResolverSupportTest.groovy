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
package io.micronaut.testresources.core.compose

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ComposeResolverSupportTest extends Specification {

    @TempDir
    Path tempDir

    def cleanup() {
        ComposeResolverSupport.projectManager = null
    }

    def "checking whether Compose is enabled does not initialize the project manager"() {
        given:
        ComposeResolverSupport.projectManager = null

        expect:
        ComposeResolverSupport.isEnabled(["compose.enabled": true])
        ComposeResolverSupport.projectManager == null
    }

    def "resolves a matching Compose service before container fallback"() {
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
        ComposeResolverSupport.projectManager = new ComposeProjectManager(cli, new ComposeProjectParser())

        expect:
        ComposeResolverSupport.resolve(
                "redis.uri",
                [:],
                config(),
                new ComposeResolverSupport.ServiceDescriptor("redis", List.of(), 6379),
                context -> true,
                context -> "redis://" + context.hostPort()
        ).get() == "redis://127.0.0.1:16379"
        cli.commands.count { it == "up -d --wait" } == 1
    }

    def "returns empty for unmatched ambiguous and password-protected services"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache1": {
                  "image": "redis:7",
                  "environment": {
                    "REDIS_PASSWORD": "secret"
                  }
                },
                "cache2": {
                  "image": "redis:7"
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "cache1",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 16379}
                ]
              },
              {
                "Service": "cache2",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 6379, "PublishedPort": 26379}
                ]
              }
            ]
            """))
        ComposeResolverSupport.projectManager = new ComposeProjectManager(cli, new ComposeProjectParser())

        expect:
        ComposeResolverSupport.resolve(
                "redis.uri",
                [:],
                config(),
                new ComposeResolverSupport.ServiceDescriptor("redis", List.of(), 6379),
                context -> !context.hasEnvironment("REDIS_PASSWORD"),
                context -> "redis://" + context.hostPort()
        ).get() == "redis://127.0.0.1:26379"
        ComposeResolverSupport.resolve(
                "redis.uri",
                [:],
                config(),
                new ComposeResolverSupport.ServiceDescriptor("redis", List.of(), 6379),
                context -> true,
                context -> "redis://" + context.hostPort()
        ).empty
        ComposeResolverSupport.resolve(
                "kafka.bootstrap.servers",
                [:],
                config(),
                new ComposeResolverSupport.ServiceDescriptor("kafka", List.of("redpanda"), 9092),
                context -> true,
                context -> "PLAINTEXT://" + context.hostPort()
        ).empty
    }

    def "resolves database properties using shared Compose database descriptors"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "mysql": {
                  "image": "mysql:8",
                  "environment": {
                    "MYSQL_USER": "demo",
                    "MYSQL_PASSWORD": "secret",
                    "MYSQL_DATABASE": "inventory"
                  },
                  "labels": {
                    "io.micronaut.test-resources.datasource": "inventory"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "mysql",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 3306, "PublishedPort": 13306}
                ]
              }
            ]
            """))
        ComposeResolverSupport.projectManager = new ComposeProjectManager(cli, new ComposeProjectParser())
        def properties = ["datasources.inventory.db-name": "inventory_test"]

        expect:
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.inventory.url",
                properties,
                config(),
                ComposeDatabaseDescriptors.MYSQL
        ).get() == "jdbc:mysql://127.0.0.1:13306/inventory_test"
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.inventory.username",
                properties,
                config(),
                ComposeDatabaseDescriptors.MYSQL
        ).get() == "demo"
        ComposeDatabaseResolverSupport.resolveR2dbc(
                "r2dbc.datasources.inventory.url",
                properties,
                config(),
                ComposeDatabaseDescriptors.MYSQL
        ).get() == "r2dbc:mysql://127.0.0.1:13306/inventory_test"
        ComposeDatabaseResolverSupport.resolveHibernateReactive(
                "jpa.inventory.properties.hibernate.connection.password",
                properties,
                config(),
                ComposeDatabaseDescriptors.MYSQL
        ).get() == "secret"
    }

    def "core exposes safe accessors for compose ports without leaking internal types"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
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
                  {"URL": "127.0.0.1", "TargetPort": 10000, "PublishedPort": 11000},
                  {"URL": "127.0.0.1", "TargetPort": 10001, "PublishedPort": 11001}
                ]
              }
            ]
            """))
        ComposeResolverSupport.projectManager = new ComposeProjectManager(cli, new ComposeProjectParser())

        expect:
        ComposeResolverSupport.resolve(
                "azure.credential.storage-shared-key.connection-string",
                [:],
                config(),
                new ComposeResolverSupport.ServiceDescriptor("azurite", List.of("azure-storage"), 10000),
                context -> true,
                context -> context.host() + ":" + context.publishedPort() + " " + context.httpEndpoint(10001).get()
        ).get() == "127.0.0.1:11000 http://127.0.0.1:11001"
    }

    def "parses Compose configuration output and tracks lifecycle ownership"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "db": {
                  "image": "postgres:17",
                  "environment": ["POSTGRES_USER=demo", "POSTGRES_PASSWORD=secret"],
                  "labels": ["io.micronaut.test-resources.datasource=inventory"]
                },
                "cache": {
                  "image": "redis:7",
                  "labels": {
                    "io.micronaut.test-resources.ignore": "true",
                    "ignored-null": null
                  }
                }
              }
            }
            """), psJson("""
            {"Service": "db", "State": "running", "Publishers": [{"URL": "", "TargetPort": "5432", "PublishedPort": "15432"}]}
            {"Service": "cache", "State": "running"}
            """))
        cli.beforeStartPs = psJson("""
            [
              {"Service": "cache", "State": "running"}
            ]
            """)
        def manager = new ComposeProjectManager(cli, new ComposeProjectParser())

        when:
        def project = manager.getOrCreate(ComposeConfiguration.from(config()))
        manager.close()

        then:
        project.services().size() == 2
        def db = project.services().find { it.name() == "db" }
        db.environment("POSTGRES_USER", "postgres") == "demo"
        db.labels()[ComposeLabels.DATASOURCE] == "inventory"
        db.publishedPort(5432).get().host() == "localhost"
        !db.externallyManaged()
        project.services().find { it.name() == "cache" }.ignored()
        project.services().find { it.name() == "cache" }.externallyManaged()
        cli.commands.any { it == "stop db" }
        !cli.commands.any { it == "stop cache" }
    }

    def "falls back when compose up wait is not supported"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {
                  "image": "redis:7"
                }
              }
            }
            """), psJson("""
            [
              {
                "service": "cache",
                "state": "running",
                "publishers": [
                  {"url": "0.0.0.0", "target_port": 6379, "published_port": 16379}
                ]
              }
            ]
            """))
        cli.upWaitResult = new ComposeCommandResult(1, "", "unknown flag: --wait")
        def manager = new ComposeProjectManager(cli, new ComposeProjectParser())

        when:
        def project = manager.getOrCreate(ComposeConfiguration.from(config()))

        then:
        project.services().first().publishedPort(6379).get().host() == "localhost"
        cli.commands.contains("up -d --wait")
        cli.commands.contains("up -d")
    }

    def "reports compose command diagnostics when startup rendering or inspection fails"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "cache": {
                  "image": "redis:7"
                }
              }
            }
            """), "[]")
        cli.beforeStartPsResult = beforeStartPsResult
        cli.upWaitResult = upWaitResult
        if (configResult != null) {
            cli.configResult = configResult
        }
        if (afterStartPsResult != null) {
            cli.afterStartPsResult = afterStartPsResult
        }
        def manager = new ComposeProjectManager(cli, new ComposeProjectParser())

        when:
        manager.getOrCreate(ComposeConfiguration.from(config()))

        then:
        def e = thrown(ComposeCliException)
        e.message == message

        where:
        beforeStartPsResult                     | upWaitResult                              | configResult                               | afterStartPsResult                         | message
        new ComposeCommandResult(1, "", "none") | new ComposeCommandResult(1, "", "boom")   | null                                       | null                                       | "Docker Compose failed to start services: boom"
        null                                    | new ComposeCommandResult(0, "", "")       | new ComposeCommandResult(1, "", "bad cfg") | null                                       | "Docker Compose failed to render configuration: bad cfg"
        null                                    | new ComposeCommandResult(0, "", "")       | null                                       | new ComposeCommandResult(1, "", "bad ps")  | "Docker Compose failed to inspect services: bad ps"
    }

    def "parses compose ps variants and ignores malformed config output"() {
        given:
        def parser = new ComposeProjectParser()

        expect:
        parser.parse("", "[]", Set.of()).empty
        parser.parse("not-json", "[]", Set.of()).empty
        parser.runningServices(psJson("""
            {
              "Name": "cache-1",
              "State": "running",
              "Publishers": [
                {"HostIp": "::", "TargetPort": 6379, "PublishedPort": 16379}
              ]
            }
            """)) == Set.of("cache-1")
        parser.parse(configJson("""
            {
              "services": {
                "cache-1": {
                  "image": "redis:7",
                  "labels": ["io.micronaut.test-resources.service=redis"],
                  "environment": ["REDIS_PASSWORD=secret"]
                }
              }
            }
            """), psJson("""
            {
              "Name": "cache-1",
              "State": "running",
              "Publishers": [
                {"HostIp": "::", "TargetPort": 6379, "PublishedPort": 16379}
              ]
            }
            """), Set.of("cache-1")).first().publishedPort(6379).get().host() == "localhost"
    }

    def "resolves additional database URL variants and empty property matches"() {
        given:
        createComposeFile()
        def cli = new FakeComposeCli(configJson("""
            {
              "services": {
                "mssql": {
                  "image": "mcr.microsoft.com/mssql/server:2022-latest",
                  "environment": {
                    "MSSQL_DATABASE": "inventory"
                  },
                  "labels": {
                    "io.micronaut.test-resources.datasource": "inventory"
                  }
                },
                "oracle": {
                  "image": "gvenzl/oracle-free:23-slim",
                  "labels": {
                    "io.micronaut.test-resources.datasource": "oracle"
                  }
                }
              }
            }
            """), psJson("""
            [
              {
                "Service": "mssql",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 1433, "PublishedPort": 11433}
                ]
              },
              {
                "Service": "oracle",
                "State": "running",
                "Publishers": [
                  {"URL": "127.0.0.1", "TargetPort": 1521, "PublishedPort": 11521}
                ]
              }
            ]
            """))
        ComposeResolverSupport.projectManager = new ComposeProjectManager(cli, new ComposeProjectParser())

        expect:
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.inventory.url",
                [:],
                config(),
                ComposeDatabaseDescriptors.MSSQL
        ).get() == "jdbc:sqlserver://127.0.0.1:11433;databaseName=inventory;encrypt=false"
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.oracle.url",
                [:],
                config(),
                ComposeDatabaseDescriptors.ORACLE
        ).get() == "jdbc:oracle:thin:@127.0.0.1:11521/freepdb1"
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.oracle.driver-class-name",
                [:],
                config(),
                ComposeDatabaseDescriptors.ORACLE
        ).get() == "oracle.jdbc.OracleDriver"
        ComposeDatabaseResolverSupport.resolveR2dbc(
                "r2dbc.datasources.inventory.password",
                [:],
                config(),
                ComposeDatabaseDescriptors.MSSQL
        ).get() == "A_Str0ng_Required_Password"
        ComposeDatabaseResolverSupport.resolveJdbc(
                "datasources.url",
                [:],
                config(),
                ComposeDatabaseDescriptors.MSSQL
        ).empty
        ComposeDatabaseResolverSupport.resolveHibernateReactive(
                "jpa.inventory.properties.hibernate.connection.username",
                [:],
                config(),
                ComposeDatabaseDescriptors.MSSQL
        ).get() == "SA"
    }

    def "builds configuration from scalar and typed values"() {
        given:
        Files.writeString(tempDir.resolve("docker-compose.yaml"), "services: {}\n")

        expect:
        def scalar = ComposeConfiguration.from([
                "compose.enabled": "true",
                "compose.working-directory": tempDir.toString(),
                "compose.files": "compose.yml, docker-compose.yml",
                "compose.profiles": "dev,test",
                "compose.start": "false",
                "compose.stop-managed": "false",
                "compose.startup-timeout": "2m"
        ], ["micronaut.test.resources.scope": "test scope"])
        scalar.enabled()
        scalar.files() == [
                tempDir.resolve("compose.yml").toAbsolutePath().normalize(),
                tempDir.resolve("docker-compose.yml").toAbsolutePath().normalize()
        ]
        scalar.profiles() == ["dev", "test"]
        scalar.projectName().startsWith("mn-tr-")
        !scalar.start()
        !scalar.stopManaged()
        scalar.startupTimeout() == java.time.Duration.ofMinutes(2)

        def typed = ComposeConfiguration.from([
                "compose.enabled": true,
                "compose.working-directory": tempDir,
                "compose.profiles": ["dev", "", " test "],
                "compose.startup-timeout": 30
        ])
        typed.usable()
        typed.files() == [tempDir.resolve("docker-compose.yaml").toAbsolutePath().normalize()]
        typed.profiles() == ["dev", "test"]
        typed.startupTimeout() == java.time.Duration.ofSeconds(30)
    }

    def "default compose cli builds command and captures process output"() {
        given:
        createComposeFile()
        def docker = executableScript("docker", '''
            #!/usr/bin/env bash
            printf 'PWD=%s\\n' "$PWD"
            printf 'ARGS=%s\\n' "$*"
            printf 'PROFILES=%s\\n' "${COMPOSE_PROFILES:-}"
            printf 'diagnostic from stderr\\n' >&2
        ''')
        def configuration = ComposeConfiguration.from(config() + ["compose.profiles": ["dev", "test"]])

        when:
        def result = new ComposeCli.Default(List.of(docker.toString(), "compose")).run(configuration, ["ps", "--format", "json"])

        then:
        result.successful()
        result.output().contains("PWD=${tempDir.toAbsolutePath().normalize()}")
        result.output().contains("ARGS=compose -f ${tempDir.resolve("compose.yml").toAbsolutePath().normalize()} --project-name mn-compose-test --profile dev --profile test ps --format json")
        result.output().contains("PROFILES=dev,test")
        result.error() == "diagnostic from stderr\n"
        result.diagnostic() == "diagnostic from stderr"
    }

    def "default compose cli reports missing executable and timeouts"() {
        given:
        createComposeFile()
        def timeoutScript = executableScript("slow-docker", '''
            #!/usr/bin/env bash
            sleep 5
        ''')
        def configuration = ComposeConfiguration.from(config() + ["compose.startup-timeout": "10ms"])

        when:
        new ComposeCli.Default(List.of(tempDir.resolve("missing-docker").toString())).run(configuration, ["ps"])

        then:
        def missing = thrown(ComposeCliException)
        missing.message.contains("Docker Compose V2 is not available")

        when:
        new ComposeCli.Default(List.of(timeoutScript.toString())).run(configuration, ["ps"])

        then:
        def timeout = thrown(ComposeCliException)
        timeout.message == "Docker Compose command timed out"
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

    private Path executableScript(String name, String contents) {
        def script = tempDir.resolve(name)
        Files.writeString(script, contents.stripIndent().trim() + "\n")
        script.toFile().setExecutable(true)
        script
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
        ComposeCommandResult beforeStartPsResult
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
                    return beforeStartPsResult ?: new ComposeCommandResult(0, beforeStartPs, "")
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
                return configResult ?: new ComposeCommandResult(0, config, "")
            }
            if (arguments[0] == "stop") {
                return stopResult
            }
            throw new IllegalArgumentException("Unexpected command: $arguments")
        }
    }
}
