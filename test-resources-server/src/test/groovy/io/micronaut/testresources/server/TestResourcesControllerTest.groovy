package io.micronaut.testresources.server

import io.micronaut.context.annotation.Property
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.client.TestResourcesClient
import jakarta.inject.Inject
import spock.lang.Specification
import io.micronaut.testresources.testcontainers.TestContainers

@MicronautTest
@Property(name = "micronaut.testresources.server.url", value = "")
@Property(name = "micronaut.http.client.read-timeout", value = "60s")
class TestResourcesControllerTest extends Specification {

    @Inject
    DiagnosticsClient client

    def "verifies that server can instantiate container"() {
        expect:
        client.resolvableProperties ==~ ['kafka.bootstrap.servers', 'failing.message', 'failing.container']
        client.listContainers().empty

        when:
        client.resolve("kafka.bootstrap.servers", [:], [:])

        then:
        def containers = client.listContainers()
        containers.size() == 1
        containers[0].imageName.startsWith 'confluentinc/cp-kafka'

        when:
        client.closeAll()

        then:
        client.listContainers().empty
    }

    def "can resolve a valid property after an error in a regular test resource"() {
        expect:
        client.resolvableProperties ==~ ['kafka.bootstrap.servers', 'failing.message', 'failing.container']
        client.listContainers().empty

        when: "resolution of a property fails"
        client.resolve("failing.message", [:], [:])

        then:
        RuntimeException ex = thrown()

        when: "resolves another property"
        client.resolve("kafka.bootstrap.servers", [:], [:])

        then:
        def containers = client.listContainers()
        containers.size() == 1
        containers[0].imageName.startsWith 'confluentinc/cp-kafka'

        when:
        client.closeAll()

        then:
        client.listContainers().empty
    }

    def "can resolve a valid property after an error in a container test resource"() {
        expect:
        client.resolvableProperties ==~ ['kafka.bootstrap.servers', 'failing.message', 'failing.container']
        client.listContainers().empty

        when: "resolution of a property which starts a container"
        client.resolve("failing.container", [:], [:])

        then:
        RuntimeException ex = thrown()

        when: "resolves another property"
        client.resolve("kafka.bootstrap.servers", [:], [:])

        then:
        def containers = client.listContainers()
        containers.size() == 1
        containers[0].imageName.startsWith 'confluentinc/cp-kafka'

        when:
        client.closeAll()

        then:
        client.listContainers().empty
    }

    @Client("/")
    static interface DiagnosticsClient extends TestResourcesClient {
        @Get("/testcontainers")
        List<TestContainers> listContainers();

        @Override
        @Post("/list")
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);

        @Override
        @Post("/resolve")
        Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig);

        @Override
        @Get("/requirements/expr/{expression}")
        List<String> getRequiredProperties(String expression);

        @Override
        @Get("/requirements/entries")
        List<String> getRequiredPropertyEntries();

        /**
         * Closes all test resources.
         */
        @Get("/close/all")
        boolean closeAll();

        @Get("/close/{id}")
        boolean closeScope(@Nullable String id);
    }
}
