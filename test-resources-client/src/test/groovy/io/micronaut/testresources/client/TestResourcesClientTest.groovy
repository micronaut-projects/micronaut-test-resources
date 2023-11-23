package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.TestResourcesResolutionException
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
        TestResourcesResolutionException e = thrown()
        e.message == "Test resources doesn't support resolving expression 'missing'"
    }

    @RestoreSystemProperties
    def "reasonable error message when the server throws an error"() {
        def app = createApplication()

        when:
        app.getProperty("throws", String)

        then:
        TestResourcesException e = thrown()
        e.message == "Test resources service wasn't able to revolve expression 'throws': Something bad happened"
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
