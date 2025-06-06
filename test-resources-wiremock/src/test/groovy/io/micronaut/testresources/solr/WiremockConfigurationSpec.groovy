package io.micronaut.testresources.solr

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared

@MicronautTest
class WiremockConfigurationSpec extends AbstractWiremockSpec {

    @Inject
    ApplicationContext applicationContext

    @Shared
    String testResourcePath = 'src/test/resources'

    def 'verify Wiremock container configuration'() {
        when: 'we get the Wiremock client'
        def client = applicationContext.getBean(WiremockClient)

        then: 'container is running'
        listContainers().size() == 1

        and: 'Port is configured'
        client.port < 65536 && client.port > 0

        and: 'Host is configured'
        client.host == 'localhost'

        and: 'URL is configured'
        client.url == "http://${client.host}:${client.port}"

        and: 'The container is using the correct image'
        with(TestContainers.listByScope('wiremock').get(Scope.of('wiremock'))) {
            size() == 1
            get(0).dockerImageName == 'wiremock/wiremock:3.13.0'
        }
    }
}
