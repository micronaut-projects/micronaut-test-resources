package io.micronaut.testresources.wiremock

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest(environments = "cli-args-scalar")
class WireMockCliArgsScalarSpec extends AbstractWireMockSpec {

    def "passes configured scalar CLI arg to WireMock"() {
        when:
        def client = wireMockClient()

        then:
        client.url

        and:
        with(wireMockContainers()) {
            size() == 1
            get(0).commandParts.contains('--verbose')
        }
    }
}
