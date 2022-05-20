package io.micronaut.testresources.proxy

import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.client.TestResourcesClient
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "micronaut.testresources.proxy.url", value = "")
class TestResourcesControllerTest extends Specification {

    @Inject
    DiagnosticsClient client

    def "verifies that proxy can instantiate container"() {
        expect:
        client.resolvableProperties == ['kafka.bootstrap.servers']
        client.listContainers().empty

        when:
        client.resolve("kafka.bootstrap.servers", [:])

        then:
        def containers = client.listContainers()
        containers.size() == 1
        containers[0].imageName.startsWith 'confluentinc/cp-kafka'

        when:
        client.closeAll()

        then:
        client.listContainers().empty
    }

    @Client("/proxy")
    static interface DiagnosticsClient extends TestResourcesClient {
        @Get("/testcontainers")
        List<TestContainer> listContainers();
    }
}
