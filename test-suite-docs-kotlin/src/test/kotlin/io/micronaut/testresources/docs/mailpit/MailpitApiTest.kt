package io.micronaut.testresources.docs.mailpit

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.net.URI
// end::imports[]
import io.micronaut.context.annotation.Property
import org.junit.jupiter.api.Assertions.assertEquals

@Property(name = "mailpit.api.url", value = "http://localhost:8025")
// tag::clazz[]
@MicronautTest
class MailpitApiTest {

    @Value("\${mailpit.api.url}") // <1>
    lateinit var mailpitApiUrl: URI

    @Test
    fun theMessagesAreListedThroughTheMailpitApi() {
        val request: HttpRequest<*> = HttpRequest.GET<Any>(mailpitApiUrl.resolve("/api/v1/messages")) // <2>
        // end::clazz[]
        assertEquals(URI.create("http://localhost:8025/api/v1/messages"), request.uri)
        // tag::clazz[]
    }
}
// end::clazz[]
