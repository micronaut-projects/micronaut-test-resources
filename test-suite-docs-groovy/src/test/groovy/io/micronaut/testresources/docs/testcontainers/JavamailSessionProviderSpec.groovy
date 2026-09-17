package io.micronaut.testresources.docs.testcontainers

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "smtp.host", value = "localhost")
@Property(name = "smtp.port", value = "2525")
class JavamailSessionProviderSpec extends Specification {

    @Inject
    JavamailSessionProvider sessionProvider

    void "the session uses the container host and port"() {
        when:
        def session = sessionProvider.session()

        then:
        session.getProperty("mail.smtp.host") == "localhost"
        session.getProperty("mail.smtp.port") == "2525"
    }
}
