package io.micronaut.testresources.hashicorp.consul

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import org.testcontainers.DockerClientFactory

@MicronautTest
class ConsulStartedTest extends AbstractTestContainersSpec {

    @Value('${consul.client.host}')
    String host

    @Value('${consul.client.port}')
    int port

    @Value('${consul.client.default-zone}')
    String defaultZone

    @Override
    String getScopeName() {
        'consul'
    }

    def "automatically starts a Consul container"() {
        given:
        // host is different on different platforms (localhost on linux, but 127.0.0.1 on osx)
        def dockerHost = DockerClientFactory.instance().dockerHostIpAddress()

        expect:
        dockerHost in ["localhost", "127.0.0.1"]
        dockerHost == host
        listContainers().collectMany { it.ports as List }.any { defaultZone == "$dockerHost:$it.publicPort" }
        listContainers().collectMany { it.ports as List }.any { port == it.publicPort }
    }
}
