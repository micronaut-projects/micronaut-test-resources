package io.micronaut.testresources.docs.testcontainers;

// tag::imports[]
import io.micronaut.context.annotation.Value;
import io.micronaut.email.javamail.sender.SessionProvider;
import jakarta.inject.Singleton;
import jakarta.mail.Session;

import java.util.Properties;
// end::imports[]

// tag::clazz[]
@Singleton
public class JavamailSessionProvider implements SessionProvider {
    @Value("${smtp.host}")          // <1>
    private String smtpHost;

    @Value("${smtp.port}")          // <2>
    private String smtpPort;

    @Override
    public Session session() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        return Session.getDefaultInstance(props);
    }
}
// end::clazz[]
