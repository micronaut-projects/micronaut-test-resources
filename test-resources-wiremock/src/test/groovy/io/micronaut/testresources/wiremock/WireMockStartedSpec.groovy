package io.micronaut.testresources.wiremock

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest
class WireMockStartedSpec extends AbstractWireMockSpec {

    def "starts one WireMock container and resolves connection properties"() {
        when:
        def client = wireMockClient()

        then:
        listContainers().size() == 1
        client.host
        client.port > 0
        client.port < 65536
        client.url == "http://${client.host}:${client.port}"

        and:
        with(wireMockContainers()) {
            size() == 1
            get(0).dockerImageName == WireMockTestResourceProvider.DEFAULT_IMAGE
        }
    }
}
