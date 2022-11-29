package io.micronaut.testresources.testcontainers

import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

import java.time.Duration

class TestContainerMetadataSupportTest extends Specification {

    def "reads image name"() {
        def config = """
                containers:
                    foo:
                        image-name: some/image
                    bar:
                        image-name: some/other/image
        """

        when:
        def md1 = metadataFrom(config, "foo")
        def md2 = metadataFrom(config, "bar")

        then:
        md1.present
        md1.get().with {
            assert it.imageName.get() == "some/image"
        }
        md2.present
        md2.get().with {
            assert it.imageName.get() == "some/other/image"
        }
    }

    def "reads hostnames"() {
        def config = """
                containers:
                    foo:
                        hostnames: some.host.name
                    bar:
                        hostnames:
                            - some.host.name
                            - some.other.host.name
        """

        when:
        def md1 = metadataFrom(config, "foo")
        def md2 = metadataFrom(config, "bar")

        then:
        md1.present
        md1.get().with {
            assert it.hostNames == ["some.host.name"] as Set
        }
        md2.present
        md2.get().with {
            assert it.hostNames == ["some.host.name", "some.other.host.name"] as Set
        }
    }

    def "reads exposed ports"() {
        def config = """
                containers:
                    foo:
                        exposed-ports:
                          - some.port: 8080
                    bar:
                        exposed-ports:
                          - some.port: 1234
                          - some.other.port: 5678
        """

        when:
        def md1 = metadataFrom(config, "foo")
        def md2 = metadataFrom(config, "bar")

        then:
        md1.present
        md1.get().with {
            assert it.exposedPorts == ['some.port': 8080]
        }
        md2.present
        md2.get().with {
            assert it.exposedPorts == [
                    'some.port': 1234,
                    'some.other.port': 5678
            ]
        }
    }

    def "reads filesystem binds"() {
        def config = """
                containers:
                    foo:
                        ro-fs-bind:
                          - /some/path: /some/container/path
                          - classpath:/some/file.txt: /some/container/file.txt
                        rw-fs-bind:
                          - /some/other/path: /some/other/container/path
                          - "../relative": /absolute/path
                          - classpath:/some/other-file.txt: /some/container/other-file.txt
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.roFsBinds == ['/some/path': '/some/container/path', 'classpath:/some/file.txt': '/some/container/file.txt']
            assert it.rwFsBinds == ['/some/other/path': '/some/other/container/path', '../relative': '/absolute/path', 'classpath:/some/other-file.txt': '/some/container/other-file.txt']
        }
    }

    def "reads command"() {
        def config = """
                containers:
                    foo:
                        command: "./gradlew run"
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.command == ["./gradlew run"]
        }
    }

    def "reads working directory"() {
        def config = """
                containers:
                    foo:
                        working-directory: /working/directory
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.workingDirectory.get() == "/working/directory"
        }
    }

    def "reads environment variables"() {
        def config = """
                containers:
                    foo:
                        env:
                            - SOME_ENV_VAR: some value
                            - SOME_OTHER_ENV_VAR: some other value
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.env == [
                    'SOME_ENV_VAR': 'some value',
                    'SOME_OTHER_ENV_VAR': 'some other value'
            ]
        }
    }

    def "reads labels"() {
        def config = """
                containers:
                    foo:
                        labels:
                            - label1: value
                            - label2: value 2
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.labels == [
                    'label1': 'value',
                    'label2': 'value 2'
            ]
        }
    }

    def "reads startup timeout"() {
        def config = """
                containers:
                    foo:
                        startup-timeout: $duration
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.startupTimeout.get() == expectedDuration
        }

        where:
        duration | expectedDuration
        "1s"     | Duration.ofSeconds(1)
        "2m"     | Duration.ofMinutes(2)
        "3h"     | Duration.ofHours(3)
    }

    def "reads copy file to container"() {
        def config = """
                containers:
                    foo:
                        copy-to-container:
                            - classpath:/some/file.txt: /some/container/file.txt
                            - /host/path: /container/path
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            def copies = it.fileCopies
            assert copies.size() == 2
            assert copies.findAll { it.destination == "/some/container/file.txt" }.size() == 1
            assert copies.findAll { it.destination == "/container/path" }.size() == 1
        }
    }

    def "reads memory parameters"() {
        def config = """
                containers:
                    foo:
                        memory: $memory
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.memory.get() == expectedMemory
        }

        where:
        memory  | expectedMemory
        '12345' | 12345L
        '300k'  | 307200L
        '128m'  | 134217728L
        '2g'    | 2147483648L
        '2.5G'  | 2684354560L
    }

    def "reads swap memory parameters"() {
        def config = """
                containers:
                    foo:
                        swap-memory: $memory
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.swapMemory.get() == expectedMemory
        }

        where:
        memory  | expectedMemory
        '12345' | 12345L
        '300k'  | 307200L
        '128m'  | 134217728L
        '2g'    | 2147483648L
        '2.5G'  | 2684354560L
    }

    def "reads shared memory parameters"() {
        def config = """
                containers:
                    foo:
                        shared-memory: $memory
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            assert it.sharedMemory.get() == expectedMemory
        }

        where:
        memory  | expectedMemory
        '12345' | 12345L
        '300k'  | 307200L
        '128m'  | 134217728L
        '2g'    | 2147483648L
        '2.5G'  | 2684354560L
    }

    def "reads networks"() {
        def config = """
                containers:
                    foo:
                        network: first
                        network-aliases: main
                    bar:
                        network: second
                        network-aliases:
                            - tarzan
                            - jane
        """

        when:
        def md1 = metadataFrom(config, "foo")
        def md2 = metadataFrom(config, "bar")

        then:
        md1.present
        md1.get().with {
            assert it.network.get() == 'first'
            assert it.networkAliases == ['main'] as Set
        }
        md2.present
        md2.get().with {
            assert it.network.get() == 'second'
            assert it.networkAliases == ['tarzan', 'jane'] as Set
        }
    }

    def "reads log wait strategy"() {
        def config = """
                containers:
                    foo:
                        wait-strategy:
                            log:
                                regex: ".*some log message.*"
                                times: 4
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof LogMessageWaitStrategy
        }
    }

    def "reads http wait strategy"() {
        def config = """
                containers:
                    foo:
                        wait-strategy:
                            http:
                                path: /
                                status-code: 200
                                port: 8181
                                tls: true
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof HttpWaitStrategy
        }
    }

    def "reads port wait strategy"() {
        def config = """
                containers:
                    foo:
                        wait-strategy: port
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof HostPortWaitStrategy
        }
    }

    def "reads healthcheck wait strategy"() {
        def config = """
                containers:
                    foo:
                        wait-strategy: healthcheck
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        md.get().with {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof DockerHealthcheckWaitStrategy
        }
    }


    private static Optional<TestContainerMetadata> metadataFrom(String yaml, String key) {
        def asMap = convert(yaml)
        TestContainerMetadataSupport.convertToMetadata(asMap, key)
    }

    private static Map<String, Object> convert(String text) {
        Yaml yaml = new Yaml()
        def object = yaml.load(text.stripMargin())
        flatten(object, "", [:])
    }

    private static Map<String, Object> flatten(Object input, String prefix, Map<String, Object> output) {
        if (input instanceof Map) {
            input.each { k, v ->
                flatten(v, prefix ? "${prefix}.${k}" : k, output)
            }
        } else {
            output.put(prefix, input)
        }
        output
    }
}
