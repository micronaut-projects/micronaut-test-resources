package io.micronaut.testresources.server

import io.micronaut.context.annotation.Property
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.client.TestResourcesClient
import io.micronaut.testresources.codec.Result
import io.micronaut.testresources.codec.TestResourcesMediaType
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "micronaut.testresources.server.url", value = "")
@Property(name = "micronaut.http.client.read-timeout", value = "60s")
class TestResourcesControllerTest extends Specification {

    @Inject
    DiagnosticsClient client

    def "verifies that server can instantiate container"() {
        expect:
        client.resolvableProperties.value() == ['kafka.bootstrap.servers']
        client.listContainers().value().empty

        when:
        Optional<Result<String>> servers = client.resolve("kafka.bootstrap.servers", [:], [:])

        then:
        servers.present
        def containers = client.listContainers().value()
        containers.size() == 1
        containers[0].imageName.startsWith 'confluentinc/cp-kafka'

        when:
        client.closeAll()

        then:
        client.listContainers().value().empty
    }

    @Client("/")
    @Produces(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    @Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
    static interface DiagnosticsClient extends TestResourcesClient {
        @Get("/testcontainers")
        Result<List<TestContainer>> listContainers();

        @Override
        @Post("/list")
        Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);

        @Override
        @Post("/resolve")
        Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig);

        @Override
        @Get("/requirements/expr/{expression}")
        Result<List<String>> getRequiredProperties(String expression);

        @Override
        @Get("/requirements/entries")
        Result<List<String>> getRequiredPropertyEntries();

        /**
         * Closes all test resources.
         */
        @Get("/close/all")
        Result<Boolean> closeAll();

        @Get("/close/{id}")
        Result<Boolean> closeScope(@Nullable String id);
    }
}
