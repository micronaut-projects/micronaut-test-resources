package io.micronaut.testresources.wiremock

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest(environments = "cli-args")
class WireMockCliArgsSpec extends AbstractWireMockSpec {

    def "passes configured CLI args to WireMock"() {
        when:
        def client = wireMockClient()

        then:
        client.url

        and:
        with(wireMockContainers()) {
            size() == 1
            get(0).commandParts.contains('--verbose')
            get(0).commandParts.contains('--global-response-templating')
        }
    }
}
