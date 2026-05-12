package io.micronaut.testresources.mailpit

import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.util.concurrent.PollingConditions

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@MicronautTest
class MailpitStartedTest extends AbstractMailpitSpec {

    @Inject
    EmailSender<?, ?> emailSender

    @Inject
    @Value('${mailpit.api.url}')
    String mailpitApiUrl

    def "automatically starts a Mailpit container for JavaMail"() {
        given:
        def subject = "Hello from Micronaut Test Resources"

        when:
        emailSender.send(
                Email.builder()
                        .from("author@from.domain")
                        .to("receiver@to.domain")
                        .subject(subject)
                        .body("This is the email body")
        )

        then:
        listContainers().size() == 1
        new PollingConditions(timeout: 30).eventually {
            assert capturedMessages().contains(subject)
        }
    }

    private String capturedMessages() {
        def request = HttpRequest.newBuilder(URI.create(mailpitApiUrl + "/messages")).GET().build()
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body()
    }
}
