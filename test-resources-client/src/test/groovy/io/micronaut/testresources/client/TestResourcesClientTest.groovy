package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
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
    EmbeddedServer proxy

    def "property source loader registers properties from proxy"() {
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
            ${TestResourcesClient.PROXY_URI}=${proxy.getURI()}
        """.stripIndent()
        def app = ApplicationContext.builder()
                .classLoader(cl)
                .properties(['proxy': 'false'])
                .start()
        assert !app.findBean(TestProxy).present
        return app
    }

    @Controller("/proxy")
    @Requires(property = 'proxy', notEquals = 'false')
    static class TestProxy implements TestResourcesResolver {

        @Override
        @Get("/list")
        List<String> getResolvableProperties() {
            ["dummy1", "dummy2"]
        }

        @Override
        @Get("/requirements")
        List<String> getRequiredProperties() {
            []
        }

        @Override
        @Put('/resolve')
        Optional<String> resolve(String name, Map<String, Object> properties) {
            Optional.of("value for $name".toString())
        }

        @Get("/close/all")
        void closeAll() {

        }
    }
}
