package io.micronaut.testresources.solr

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import spock.lang.Shared

@MicronautTest(environments = "cli-args")
class WiremockWithCliArgsSpec extends AbstractWiremockSpec {

    @Inject
    ApplicationContext applicationContext

    @Shared
    String testResourcePath = 'src/test/resources'

    def 'verify Wiremock container configured with cli args'() {
        when: 'we get the Wiremock client'
        def client = applicationContext.getBean(WiremockClient)

        and: 'Port is configured'
        client.port == 8080

        then: 'The cli args are passed to the container command'
        with(TestContainers.listByScope('wiremock').get(Scope.of('wiremock'))) {
            size() == 1
            get(0).getCommandParts().contains('--port')
            get(0).getCommandParts().contains('8080')
            get(0).getCommandParts().contains('--verbose')
            get(0).getCommandParts().contains('--global-response-templating')
        }
    }
}
