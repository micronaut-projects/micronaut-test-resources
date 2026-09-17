package io.micronaut.testresources.docs.wiremock;

// tag::imports[]
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
// end::imports[]

import io.micronaut.context.annotation.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "wiremock.url", value = "http://localhost:8080")
// tag::clazz[]
@MicronautTest
class WireMockClientTest {

    @Value("${wiremock.url}") // <1>
    String wireMockUrl;

    @Test
    void theWireMockClientIsConfiguredWithTheContainerUrl() {
        URI uri = URI.create(wireMockUrl);
        WireMock.configureFor(uri.getHost(), uri.getPort()); // <2>
        // end::clazz[]
        assertEquals("localhost", uri.getHost());
        assertEquals(8080, uri.getPort());
        // tag::clazz[]
    }
}
// end::clazz[]
