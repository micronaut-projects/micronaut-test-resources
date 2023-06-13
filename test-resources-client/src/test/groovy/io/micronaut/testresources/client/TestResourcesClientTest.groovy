package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Path

import static io.micronaut.testresources.client.ConfigFinder.systemPropertyNameOf

@MicronautTest
class TestResourcesClientTest extends Specification implements ClientCleanup {

    @TempDir
    Path tempDir

    @Inject
    EmbeddedServer server

    @RestoreSystemProperties
    def "property source loader registers properties from server"() {
        def app = createApplication()

        expect:
        app.getProperty("dummy1", String).get() == 'value for dummy1'
        app.getProperty("dummy2", String).get() == 'value for dummy2'

        when:
        app.getProperty("missing", String).empty

        then:
        ConfigurationException e = thrown()
        e.message == 'Could not resolve placeholder ${auto.test.resources.missing}'
    }

    private ApplicationContext createApplication() {
        System.setProperty(systemPropertyNameOf(TestResourcesClient.SERVER_URI), server.getURI().toString())
        def app = ApplicationContext.builder()
                .properties(['server': 'false'])
                .start()
        assert !app.findBean(TestServer).present
        return app
    }

}
