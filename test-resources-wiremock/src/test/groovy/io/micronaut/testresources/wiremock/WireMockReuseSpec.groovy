package io.micronaut.testresources.wiremock

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class WireMockReuseSpec extends AbstractWireMockSpec {

    @Inject
    ApplicationContext applicationContext

    def "reuses one WireMock container for all resolved properties"() {
        when:
        def client = applicationContext.getBean(WireMockClient)

        then:
        client.host
        client.port > 0
        client.url

        and:
        with(TestContainers.listByScope("wiremock").get(Scope.of("wiremock"))) {
            size() == 1
        }
    }
}
