package io.micronaut.testresources.docs.testcontainers;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@Property(name = "smtp.host", value = "localhost")
@Property(name = "smtp.port", value = "2525")
class JavamailSessionProviderTest {

    @Inject
    JavamailSessionProvider sessionProvider;

    @Test
    void theSessionUsesTheContainerHostAndPort() {
        var session = sessionProvider.session();
        assertEquals("localhost", session.getProperty("mail.smtp.host"));
        assertEquals("2525", session.getProperty("mail.smtp.port"));
    }
}
