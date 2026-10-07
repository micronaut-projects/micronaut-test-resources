package io.micronaut.testresources.docs.testcontainers

// tag::imports[]
import io.micronaut.context.annotation.Value
import io.micronaut.email.javamail.sender.SessionProvider
import jakarta.inject.Singleton
import jakarta.mail.Session
import java.util.Properties
// end::imports[]

// tag::clazz[]
@Singleton
class JavamailSessionProvider : SessionProvider {
    @Value("\${smtp.host}")          // <1>
    lateinit var smtpHost: String

    @Value("\${smtp.port}")          // <2>
    lateinit var smtpPort: String

    override fun session(): Session {
        val props = Properties()
        props["mail.smtp.host"] = smtpHost
        props["mail.smtp.port"] = smtpPort
        return Session.getDefaultInstance(props)
    }
}
// end::clazz[]
