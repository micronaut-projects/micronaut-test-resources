package io.micronaut.testresources.mailpit

import io.micronaut.context.ApplicationContext
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Resolves Mailpit properties through a real application context, with the embedded
 * test resources resolver, to check end to end that an application which configures
 * its own SMTP endpoint keeps it and gets no Mailpit container. The opposite case,
 * where nothing is configured and Mailpit supplies the endpoint, is covered by
 * {@link MailpitStartedTest}.
 */
class MailpitExternalSmtpEndpointTest extends Specification {

    private static final String SCOPE = 'mailpit-external-smtp'

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(
            (Scope.PROPERTY_KEY)                                  : SCOPE,
            (MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST)      : 'smtp.example.test',
            (MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT)      : 2525
    )

    void cleanupSpec() {
        TestContainers.closeScope(SCOPE)
    }

    def "keeps the configured SMTP endpoint for #property"() {
        expect:
        context.getRequiredProperty(property, String) == expected

        where:
        property                                       | expected
        MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST | 'smtp.example.test'
        MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT | '2525'
    }

    def "does not offer #property when an SMTP endpoint is configured"() {
        expect:
        !context.getProperty(property, String).present

        where:
        property << [
                MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS,
                MailpitTestResourceProvider.MAILPIT_UI_URL,
                MailpitTestResourceProvider.MAILPIT_API_URL
        ]
    }

    def "does not start a Mailpit container"() {
        expect:
        TestContainers.listByScope(SCOPE).isEmpty()
    }
}
