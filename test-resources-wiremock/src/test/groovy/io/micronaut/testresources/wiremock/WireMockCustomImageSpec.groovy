package io.micronaut.testresources.wiremock

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest
class WireMockCustomImageSpec extends AbstractWireMockSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
                "test-resources.containers.wiremock.image-name": "wiremock/wiremock:3.13.1"
        ]
    }

    def "starts WireMock using a custom image"() {
        when:
        wireMockClient()

        then:
        listContainers().size() == 1
        with(wireMockContainers()) {
            size() == 1
            get(0).dockerImageName == "wiremock/wiremock:3.13.1"
        }
    }
}
