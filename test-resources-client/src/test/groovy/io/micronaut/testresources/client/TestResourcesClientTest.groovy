package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.TestResourcesResolver
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

@MicronautTest
class TestResourcesClientTest extends Specification {

    @TempDir
    Path tempDir

    @Inject
    EmbeddedServer server

    def "property source loader registers properties from server"() {
        def app = createApplication()

        expect:
        app.getProperty("dummy1", String).get() == 'value for dummy1'
        app.getProperty("dummy2", String).get() == 'value for dummy2'

        cleanup:
        app.close()
    }

    private ApplicationContext createApplication() {
        def propertiesFile = tempDir.resolve("test-resources.properties").toFile()
        def cl = new URLClassLoader([] as URL[], this.class.classLoader) {
            @Override
            URL findResource(String name) {
                if ("/test-resources.properties" == name) {
                    return propertiesFile.toURI().toURL()
                }
                return super.findResource(name)
            }
        }

        propertiesFile << """
            ${TestResourcesClient.SERVER_URI}=${server.getURI()}
        """.stripIndent()
        def app = ApplicationContext.builder()
                .classLoader(cl)
                .properties(['server': 'false'])
                .start()
        assert !app.findBean(TestServer).present
        return app
    }

    @Controller("/")
    @Requires(property = 'server', notEquals = 'false')
    static class TestServer implements TestResourcesResolver {

        @Override
        @Post("/list")
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            ["dummy1", "dummy2"]
        }

        @Override
        @Get("/requirements/expr/{expression}")
        List<String> getRequiredProperties(String expression) {
            []
        }

        @Override
        @Get("/requirements/entries")
        List<String> getRequiredPropertyEntries() {
            []
        }

        @Override
        @Post('/resolve')
        Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
            Optional.of("value for $name".toString())
        }

        @Get("/close/all")
        void closeAll() {

        }
    }
}
