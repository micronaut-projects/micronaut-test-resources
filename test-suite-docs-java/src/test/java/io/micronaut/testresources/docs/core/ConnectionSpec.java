package io.micronaut.testresources.docs.core;

// tag::imports[]
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.testresources.TestResourcesPropertyProvider;
import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
// end::imports[]

// tag::test[]
@MicronautTest
@TestResourcesProperties(
    value = "rabbitmq.uri", // <1>
    providers = ConnectionSpec.RabbitMQProvider.class // <2>
)
class ConnectionSpec {

    @Value("${rabbitmq.servers.product-cluster.port}")
    int productClusterPort;

    @Test
    void theProductClusterPortIsDerivedFromTheRabbitMqUri() {
        assertEquals(5672, productClusterPort);
    }
    // end::test[]

    // tag::provider[]
    @ReflectiveAccess
    public static class RabbitMQProvider implements TestResourcesPropertyProvider {
        @Override
        public Map<String, String> provide(Map<String, Object> testProperties) {
            String uri = (String) testProperties.get("rabbitmq.uri"); // <1>
            return Map.of(
                "rabbitmq.servers.product-cluster.port", String.valueOf(URI.create(uri).getPort()) // <2>
            );
        }
    }
    // end::provider[]
    // tag::test[]
}
// end::test[]
