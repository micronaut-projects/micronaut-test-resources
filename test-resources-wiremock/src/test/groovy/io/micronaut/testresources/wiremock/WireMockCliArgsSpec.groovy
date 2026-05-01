package io.micronaut.testresources.wiremock

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest(environments = "cli-args")
class WireMockCliArgsSpec extends AbstractWireMockSpec {

    @Inject
    ApplicationContext applicationContext

    def "passes configured CLI args to WireMock"() {
        when:
        def client = applicationContext.getBean(WireMockClient)

        then:
        client.url

        and:
        with(TestContainers.listByScope("wiremock").get(Scope.of("wiremock"))) {
            size() == 1
            get(0).commandParts.contains('--verbose')
            get(0).commandParts.contains('--global-response-templating')
        }
    }
}
