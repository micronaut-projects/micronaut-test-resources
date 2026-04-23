package io.micronaut.testresources.testcontainers

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.Volume
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
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
        with(md1.get()) {
            imageName.get() == "some/image"
        }
        md2.present
        with(md2.get()) {
            imageName.get() == "some/other/image"
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
        with(md1.get()) {
            hostNames == ["some.host.name"] as Set
        }
        md2.present
        with(md2.get()) {
            hostNames == ["some.host.name", "some.other.host.name"] as Set
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
        with(md1.get()) {
            exposedPorts == ['some.port': 8080]
        }
        md2.present
        with(md2.get()) {
            exposedPorts == [
                    'some.port'      : 1234,
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
        with(md.get()) {
            roFsBinds == ['/some/path': '/some/container/path', 'classpath:/some/file.txt': '/some/container/file.txt']
            rwFsBinds == ['/some/other/path': '/some/other/container/path', '../relative': '/absolute/path', 'classpath:/some/other-file.txt': '/some/container/other-file.txt']
        }
    }

    def "reads tmpfs mappings"() {
        def config = """
            containers:
                foo:
                    ro-tmpfs-mappings:
                        - /some/path
                        - /some/other/path
                    rw-tmpfs-mappings:
                        - /yet/another/path
                        - /yet/another/other/path
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        with(md.get()) {
            roTmpfsMappings.containsAll(['/some/path', '/some/other/path'])
            rwTmpfsMappings.containsAll(['/yet/another/path', '/yet/another/other/path'])
        }
    }

    def "reads anonymous volumes"() {
        def config = """
            containers:
                foo:
                    ro-anonymous-volumes:
                        - /some/path
                        - /some/other/path
                    rw-anonymous-volumes:
                        - /yet/another/path
                        - /yet/another/other/path
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        with(md.get()) {
            roAnonymousVolumes.containsAll(['/some/path', '/some/other/path'])
            rwAnonymousVolumes.containsAll(['/yet/another/path', '/yet/another/other/path'])
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
        with(md.get()) {
            command == ["./gradlew run"]
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
        with(md.get()) {
            workingDirectory.get() == "/working/directory"
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
        with(md.get()) {
            env == [
                    'SOME_ENV_VAR'      : 'some value',
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
        with(md.get()) {
            labels == [
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
        with(md.get()) {
            startupTimeout.get() == expectedDuration
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
        with(md.get()) {
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
                        memory: $configuredMemory
"""
        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        with(md.get()) {
            memory.get() == expectedMemory
        }

        where:
        configuredMemory | expectedMemory
        '12345'          | 12345L
        '300k'           | 307200L
        '128m'           | 134217728L
        '2g'             | 2147483648L
        '2.5G'           | 2684354560L
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
        with(md.get()) {
            swapMemory.get() == expectedMemory
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
        with(md.get()) {
            sharedMemory.get() == expectedMemory
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
                    baz:
                        network-mode: third
        """

        when:
        def md1 = metadataFrom(config, "foo")
        def md2 = metadataFrom(config, "bar")
        def md3 = metadataFrom(config, "baz")

        then:
        md1.present
        with(md1.get()) {
            network.get() == 'first'
            networkAliases == ['main'] as Set
        }
        md2.present
        with(md2.get()) {
            network.get() == 'second'
            networkAliases == ['tarzan', 'jane'] as Set
        }
        md3.present
        md3.get().with {
            networkMode.get() == 'third'
        }
    }

    def "applies anonymous volumes without losing existing create command settings"() {
        given:
        def config = """
            containers:
                foo:
                    ro-anonymous-volumes:
                        - /ro-data
                    rw-anonymous-volumes:
                        - /rw-data
                    memory: 128m
        """
        def metadata = metadataFrom(config, "foo").get()
        def container = TestContainerMetadataSupport.applyMetadata(metadata, new GenericContainer("alpine:3.20"))
        def hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind('/host-data', new Volume('/existing-bind')))
                .withMounts([new Mount().withType(MountType.VOLUME).withTarget('/existing-data').withReadOnly(false)])
        CreateContainerCmd cmd
        cmd = [
            getHostConfig: { -> hostConfig },
            withHostConfig: { HostConfig value ->
                hostConfig = value
                cmd
            }
        ] as CreateContainerCmd

        when:
        container.createContainerCmdModifiers.each { modifier ->
            modifier.modify(cmd)
        }

        then:
        hostConfig.memory == 134217728L
        hostConfig.mounts == [
                new Mount().withType(MountType.VOLUME).withTarget('/existing-data').withReadOnly(false),
                new Mount().withType(MountType.VOLUME).withTarget('/rw-data').withReadOnly(false),
                new Mount().withType(MountType.VOLUME).withTarget('/ro-data').withReadOnly(true)
        ]
        hostConfig.binds.toList() == [
            new Bind('/host-data', new Volume('/existing-bind'))
        ]
    }

    def "rejects conflicting anonymous volume access modes for the same path"() {
        given:
        def config = """
            containers:
                foo:
                    ro-anonymous-volumes:
                        - /shared-data
                    rw-anonymous-volumes:
                        - /shared-data
        """
        def metadata = metadataFrom(config, "foo").get()
        def container = TestContainerMetadataSupport.applyMetadata(metadata, new GenericContainer("alpine:3.20"))
        def hostConfig = HostConfig.newHostConfig()
        CreateContainerCmd cmd
        cmd = [
            getHostConfig: { -> hostConfig },
            withHostConfig: { HostConfig value ->
                hostConfig = value
                cmd
            }
        ] as CreateContainerCmd

        when:
        container.createContainerCmdModifiers.each { modifier ->
            modifier.modify(cmd)
        }

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Anonymous volumes cannot be declared as both read-write and read-only: [/shared-data]'
    }

    def "rejects anonymous volumes that overlap existing filesystem binds"() {
        given:
        def config = """
            containers:
                foo:
                    rw-anonymous-volumes:
                        - /shared-data
        """
        def metadata = metadataFrom(config, "foo").get()
        def container = TestContainerMetadataSupport.applyMetadata(metadata, new GenericContainer("alpine:3.20"))
        def hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind('/host-data', new Volume('/shared-data')))
        CreateContainerCmd cmd
        cmd = [
            getHostConfig: { -> hostConfig },
            withHostConfig: { HostConfig value ->
                hostConfig = value
                cmd
            }
        ] as CreateContainerCmd

        when:
        container.createContainerCmdModifiers.each { modifier ->
            modifier.modify(cmd)
        }

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Anonymous volumes cannot reuse container paths already configured by mounts, filesystem binds, or tmpfs mappings: [/shared-data]'
    }

    def "rejects anonymous volumes that overlap existing tmpfs mappings"() {
        given:
        def config = """
            containers:
                foo:
                    ro-anonymous-volumes:
                        - /shared-data
        """
        def metadata = metadataFrom(config, "foo").get()
        def container = TestContainerMetadataSupport.applyMetadata(metadata, new GenericContainer("alpine:3.20"))
        def hostConfig = HostConfig.newHostConfig()
                .withTmpFs(['/shared-data': 'rw'])
        CreateContainerCmd cmd
        cmd = [
            getHostConfig: { -> hostConfig },
            withHostConfig: { HostConfig value ->
                hostConfig = value
                cmd
            }
        ] as CreateContainerCmd

        when:
        container.createContainerCmdModifiers.each { modifier ->
            modifier.modify(cmd)
        }

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Anonymous volumes cannot reuse container paths already configured by mounts, filesystem binds, or tmpfs mappings: [/shared-data]'
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
        with(md.get()) {
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
        with(md.get()) {
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
        with(md.get()) {
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
        with(md.get()) {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof DockerHealthcheckWaitStrategy
        }
    }

    def "supports multiple wait strategies"() {
        def config = """
                containers:
                    foo:
                        wait-strategy:
                            healthcheck:
                            log:
                                regex: ".*some log message.*"
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        md.present
        with(md.get()) {
            def strategy = it.waitStrategy.get()
            assert strategy instanceof WaitAllStrategy
        }
    }

    def "can configure the all strategy when using multiple wait strategies"() {
        def config = """
                containers:
                    foo:
                        wait-strategy:
                            all:
                                mode: WITH_INDIVIDUAL_TIMEOUTS_ONLY
                                timeout: 30s
                            healthcheck:
                            log:
                                regex: ".*some log message.*"
        """

        when:
        def md = metadataFrom(config, "foo")

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Changing startup timeout is not supported with mode WITH_INDIVIDUAL_TIMEOUTS_ONLY"
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
