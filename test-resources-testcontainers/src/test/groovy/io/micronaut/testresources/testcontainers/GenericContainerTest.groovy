package io.micronaut.testresources.testcontainers

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import io.micronaut.email.javamail.sender.SessionProvider
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import jakarta.mail.Session

@MicronautTest
class GenericContainerTest extends Specification {

    @Inject
    ApplicationContext applicationContext

    @Inject
    EmailSender<?, ?> emailSender

    def "starts generic container"() {
        when:
        emailSender.send(
                Email.builder()
                        .from("author@from.domain")
                        .to("receiver@to.domain")
                        .subject("Hello, world!")
                        .body("This is the email body")
        )

        then:
        noExceptionThrown()
    }

    @ConfigurationProperties("smtp")
    static class EmailServerConfiguration {
        String host
        int port
    }

    @Singleton
    static class EmailSessionProvider implements SessionProvider {

        @Inject
        EmailServerConfiguration config

        @Override
        Session session() {
            def properties = new Properties()
            properties.put("mail.smtp.host", config.host)
            properties.put("mail.smtp.port", config.port)
            Session.getDefaultInstance(properties)
        }

    }
}
