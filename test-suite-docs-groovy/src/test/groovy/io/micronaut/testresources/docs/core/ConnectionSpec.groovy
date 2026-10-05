package io.micronaut.testresources.docs.core

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.extensions.testresources.TestResourcesPropertyProvider
import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties
import spock.lang.Specification
// end::imports[]

// tag::test[]
@MicronautTest
@TestResourcesProperties(
    value = "rabbitmq.uri", // <1>
    providers = ConnectionSpec.RabbitMQProvider // <2>
)
class ConnectionSpec extends Specification {

    @Value('${rabbitmq.servers.product-cluster.port}')
    int productClusterPort

    void "the product cluster port is derived from the RabbitMQ uri"() {
        expect:
        productClusterPort == 5672
    }
    // end::test[]

    // tag::provider[]
    @ReflectiveAccess
    static class RabbitMQProvider implements TestResourcesPropertyProvider {
        @Override
        Map<String, String> provide(Map<String, Object> testProperties) {
            String uri = (String) testProperties.get("rabbitmq.uri") // <1>
            return [
                "rabbitmq.servers.product-cluster.port": String.valueOf(URI.create(uri).port) // <2>
            ]
        }
    }
    // end::provider[]
    // tag::test[]
}
// end::test[]
