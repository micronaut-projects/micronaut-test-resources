package io.micronaut.testresources.docs.wiremock

// tag::imports[]
import com.github.tomakehurst.wiremock.client.WireMock
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.net.URI
// end::imports[]
import io.micronaut.context.annotation.Property
import org.junit.jupiter.api.Assertions.assertEquals

@Property(name = "wiremock.url", value = "http://localhost:8080")
// tag::clazz[]
@MicronautTest
class WireMockClientTest {

    @Value("\${wiremock.url}") // <1>
    lateinit var wireMockUrl: String

    @Test
    fun theWireMockClientIsConfiguredWithTheContainerUrl() {
        val uri = URI.create(wireMockUrl)
        WireMock.configureFor(uri.host, uri.port) // <2>
        // end::clazz[]
        assertEquals("localhost", uri.host)
        assertEquals(8080, uri.port)
        // tag::clazz[]
    }
}
// end::clazz[]
