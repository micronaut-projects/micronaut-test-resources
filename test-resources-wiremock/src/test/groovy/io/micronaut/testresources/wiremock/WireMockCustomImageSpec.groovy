package io.micronaut.testresources.wiremock

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class WireMockCustomImageSpec extends AbstractWireMockSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
                "test-resources.containers.wiremock.image-name": "wiremock/wiremock:3.13.1"
        ]
    }

    @Inject
    ApplicationContext applicationContext

    def "starts WireMock using a custom image"() {
        when:
        applicationContext.getBean(WireMockClient)

        then:
        listContainers().size() == 1
        with(TestContainers.listByScope("wiremock").get(Scope.of("wiremock"))) {
            size() == 1
            get(0).dockerImageName == "wiremock/wiremock:3.13.1"
        }
    }
}
