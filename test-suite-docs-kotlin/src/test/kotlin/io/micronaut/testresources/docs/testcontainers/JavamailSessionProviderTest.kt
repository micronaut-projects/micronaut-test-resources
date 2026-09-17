package io.micronaut.testresources.docs.testcontainers

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
@Property(name = "smtp.host", value = "localhost")
@Property(name = "smtp.port", value = "2525")
class JavamailSessionProviderTest {

    @Inject
    lateinit var sessionProvider: JavamailSessionProvider

    @Test
    fun theSessionUsesTheContainerHostAndPort() {
        val session = sessionProvider.session()
        assertEquals("localhost", session.getProperty("mail.smtp.host"))
        assertEquals("2525", session.getProperty("mail.smtp.port"))
    }
}
