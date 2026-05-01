package io.micronaut.testresources.wiremock

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class WireMockStartedSpec extends AbstractWireMockSpec {

    @Inject
    ApplicationContext applicationContext

    def "starts WireMock and resolves connection properties"() {
        when:
        def client = applicationContext.getBean(WireMockClient)

        then:
        listContainers().size() == 1
        client.host
        client.port > 0
        client.port < 65536
        client.url == "http://${client.host}:${client.port}"

        and:
        with(TestContainers.listByScope("wiremock").get(Scope.of("wiremock"))) {
            size() == 1
            get(0).dockerImageName == WireMockTestResourceProvider.DEFAULT_IMAGE
        }
    }
}
