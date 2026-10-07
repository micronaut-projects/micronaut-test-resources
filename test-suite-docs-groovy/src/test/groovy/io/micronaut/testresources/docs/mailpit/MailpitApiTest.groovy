package io.micronaut.testresources.docs.mailpit

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
// end::imports[]
import io.micronaut.context.annotation.Property

@Property(name = "mailpit.api.url", value = "http://localhost:8025")
// tag::clazz[]
@MicronautTest
class MailpitApiTest extends Specification {

    @Value('${mailpit.api.url}') // <1>
    URI mailpitApiUrl

    void "the messages are listed through the Mailpit API"() {
        when:
        HttpRequest<?> request = HttpRequest.GET(mailpitApiUrl.resolve("/api/v1/messages")) // <2>

        then:
        // end::clazz[]
        request.uri == URI.create("http://localhost:8025/api/v1/messages")
        // tag::clazz[]
        request.uri.path == "/api/v1/messages"
    }
}
// end::clazz[]
