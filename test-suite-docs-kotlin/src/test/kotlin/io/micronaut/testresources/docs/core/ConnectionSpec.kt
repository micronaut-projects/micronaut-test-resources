package io.micronaut.testresources.docs.core

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.extensions.testresources.TestResourcesPropertyProvider
import io.micronaut.test.extensions.testresources.annotation.TestResourcesProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
// end::imports[]

// tag::test[]
@MicronautTest
@TestResourcesProperties(
    value = ["rabbitmq.uri"], // <1>
    providers = [ConnectionSpec.RabbitMQProvider::class] // <2>
)
class ConnectionSpec {

    @Value("\${rabbitmq.servers.product-cluster.port}")
    var productClusterPort: Int = 0

    @Test
    fun theProductClusterPortIsDerivedFromTheRabbitMqUri() {
        assertEquals(5672, productClusterPort)
    }
    // end::test[]

    // tag::provider[]
    @ReflectiveAccess
    class RabbitMQProvider : TestResourcesPropertyProvider {
        override fun provide(testProperties: Map<String, Any>): Map<String, String> {
            val uri = testProperties["rabbitmq.uri"] as String // <1>
            return mapOf(
                "rabbitmq.servers.product-cluster.port" to URI.create(uri).port.toString() // <2>
            )
        }
    }
    // end::provider[]
    // tag::test[]
}
// end::test[]
