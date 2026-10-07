package io.micronaut.testresources.docs.wiremock

// tag::imports[]
import com.github.tomakehurst.wiremock.client.WireMock
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
// end::imports[]
import io.micronaut.context.annotation.Property

@Property(name = "wiremock.url", value = "http://localhost:8080")
// tag::clazz[]
@MicronautTest
class WireMockClientTest extends Specification {

    @Value('${wiremock.url}') // <1>
    String wireMockUrl

    void "the WireMock client is configured with the container url"() {
        given:
        URI uri = URI.create(wireMockUrl)

        when:
        WireMock.configureFor(uri.host, uri.port) // <2>

        then:
        // end::clazz[]
        uri.host == "localhost"
        uri.port == 8080
        // tag::clazz[]
        noExceptionThrown()
    }
}
// end::clazz[]
