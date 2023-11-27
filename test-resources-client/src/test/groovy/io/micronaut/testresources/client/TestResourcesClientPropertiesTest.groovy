package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.TestResourcesResolutionException
import jakarta.inject.Inject
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@MicronautTest
class TestResourcesClientPropertiesTest extends Specification implements ClientCleanup {

    @Inject
    EmbeddedServer server

    @RestoreSystemProperties
    def "client can be configured from system properties"() {
        System.setProperty("micronaut.test.resources.server.uri", server.getURI().toString())

        when:
        def app = createApplication()

        then:
        app.getProperty("dummy1", String).get() == 'value for dummy1'
        app.getProperty("dummy2", String).get() == 'value for dummy2'

        when:
        app.getProperty("missing", String).empty

        then:
        TestResourcesResolutionException e = thrown()
        e.message == "Test resources doesn't support resolving expression 'missing'"

        cleanup:
        System.clearProperty("micronaut.test.resources.server.uri")
    }

    private ApplicationContext createApplication() {
        def app = ApplicationContext.builder()
                .properties(['server': 'false'])
                .start()
        assert !app.findBean(TestServer).present
        return app
    }


}
