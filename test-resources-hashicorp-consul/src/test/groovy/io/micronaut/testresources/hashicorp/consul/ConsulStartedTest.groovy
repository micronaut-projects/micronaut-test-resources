package io.micronaut.testresources.hashicorp.consul

import io.micronaut.context.annotation.Value
import io.micronaut.discovery.consul.client.v1.ConsulClient
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject
import org.testcontainers.DockerClientFactory
import reactor.core.publisher.Flux

@MicronautTest
class ConsulStartedTest extends AbstractTestContainersSpec {

    @Value('${consul.client.host}')
    String host

    @Value('${consul.client.port}')
    int port

    @Value('${consul.client.default-zone}')
    String defaultZone

    @Inject
    ConsulClient consulClient

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

    def "get consul kv properties"() {
        given:
        def testKeyKeyValues = Flux.from(consulClient.readValues("test-key")).blockFirst()
        def testKey2KeyValues = Flux.from(consulClient.readValues("test-key2")).blockFirst()

        expect:
        "test-value" == new String(Base64.getDecoder().decode(testKeyKeyValues[0].value))
        "test-value2" == new String(Base64.getDecoder().decode(testKey2KeyValues[0].value))
    }
}
