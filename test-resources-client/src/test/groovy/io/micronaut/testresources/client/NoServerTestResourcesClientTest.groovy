package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.testresources.client.ConfigFinder.systemPropertyNameOf

@MicronautTest
class NoServerTestResourcesClientTest extends Specification implements ClientCleanup {

    private static final String DUMMY_URL = "https://localhost:666/nope"

    @RestoreSystemProperties
    def "reasonable error message if the server isn't running"() {

        when:
        createApplication()

        then:
        TestResourcesException e = thrown()
        e.message == "Test resource service is not available at $DUMMY_URL"
    }

    @RestoreSystemProperties
    def "system property can disable test resources client"() {
        given:
        System.setProperty(systemPropertyNameOf(TestResourcesClient.ENABLED), "false")

        when:
        def app = createApplication()

        then:
        noExceptionThrown()
        app.getProperty("datasources.default.url", String).empty

        cleanup:
        app?.close()
    }

    @RestoreSystemProperties
    def "application configuration can disable test resources client"() {
        when:
        def app = createApplication(['test-resources.enabled': false])

        then:
        noExceptionThrown()
        app.getProperty("datasources.default.url", String).empty

        cleanup:
        app?.close()
    }

    private ApplicationContext createApplication(Map<String, Object> properties = [:]) {
        System.setProperty(systemPropertyNameOf(TestResourcesClient.SERVER_URI), DUMMY_URL)
        def app = ApplicationContext.builder()
                .properties(['server': 'false'] + properties)
                .start()
        assert !app.findBean(TestServer).present
        return app
    }

}
