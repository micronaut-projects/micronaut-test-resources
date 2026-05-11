package io.micronaut.testresources.mailpit

import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class MailpitTestResourceProviderSpec extends Specification {

    private final MailpitTestResourceProvider provider = new MailpitTestResourceProvider()

    def setup() {
        provider.getRequiredProperties('unsupported.mail.property')
    }

    def "resolves JavaMail and diagnostic properties"() {
        given:
        def container = new TestMailpitContainer(2525, 8080)

        expect:
        provider.resolveProperty(MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST, container).get() == 'localhost'
        provider.resolveProperty(MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT, container).get() == '12525'
        provider.resolveProperty(MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH, container).get() == 'false'
        provider.resolveProperty(MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS, container).get() == 'false'
        provider.resolveProperty(MailpitTestResourceProvider.MAILPIT_UI_URL, container).get() == 'http://localhost:18080'
        provider.resolveProperty(MailpitTestResourceProvider.MAILPIT_API_URL, container).get() == 'http://localhost:18080/api/v1'
    }

    def "declines to start when SMTP endpoint is already configured"() {
        expect:
        !provider.shouldAnswer(
                requestedProperty,
                [(configuredProperty): 'configured'],
                [:]
        )

        where:
        requestedProperty                                      | configuredProperty
        MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST         | MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST
        MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST         | MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
        MailpitTestResourceProvider.MAILPIT_UI_URL             | MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST
        MailpitTestResourceProvider.MAILPIT_UI_URL             | MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
        MailpitTestResourceProvider.MAILPIT_API_URL            | MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST
        MailpitTestResourceProvider.MAILPIT_API_URL            | MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
        MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH         | MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST
        MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH         | MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
        MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS     | MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST
        MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS     | MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
    }

    def "requires JavaMail SMTP endpoint context before resolving non-endpoint Mailpit properties"() {
        expect:
        provider.getRequiredProperties(property) == [
                MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT
        ]

        where:
        property << [
                MailpitTestResourceProvider.MAILPIT_UI_URL,
                MailpitTestResourceProvider.MAILPIT_API_URL,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS
        ]
    }

    def "only supports Mailpit properties"() {
        expect:
        provider.shouldAnswer(MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST, [:], [:])
        !provider.shouldAnswer('smtp.host', [:], [:])
    }

    def "requires opposite JavaMail SMTP endpoint property before resolving endpoint property"() {
        expect:
        provider.getRequiredProperties(property) == requiredProperties

        where:
        property                                       | requiredProperties
        MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST | [MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT]
        MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT | [MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST]
    }

    def "clears endpoint recursion guard after recursive endpoint lookup"() {
        when:
        provider.getRequiredProperties(MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST)

        then:
        provider.getRequiredProperties(MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT) == []
        provider.getRequiredProperties(MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT) == [MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST]
    }

    def "clears endpoint recursion guard after declining explicit SMTP endpoint"() {
        given:
        provider.getRequiredProperties(MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST)

        expect:
        !provider.shouldAnswer(
                MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST,
                [(MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT): '2525'],
                [:]
        )
        provider.getRequiredProperties(MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT) == [MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST]
    }

    def "can be disabled with test resources configuration"() {
        expect:
        !provider.isEnabled(['containers.mailpit.enabled': false])
    }

    def "uses configured container ports"() {
        when:
        def container = provider.createContainer(
                DockerImageName.parse('axllent/mailpit'),
                [:],
                [
                        'containers.mailpit.smtp-port': '2525',
                        'containers.mailpit.ui-port'  : 8080
                ]
        )

        then:
        container.exposedPorts == [2525, 8080]
    }

    def "rejects invalid configured container ports"() {
        when:
        provider.createContainer(
                DockerImageName.parse('axllent/mailpit'),
                [:],
                [(property): value]
        )

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(property)

        where:
        property                          | value
        'containers.mailpit.smtp-port'    | 0
        'containers.mailpit.ui-port'      | 'not-a-port'
    }

    private static class TestMailpitContainer extends MailpitTestResourceProvider.MailpitContainer {

        TestMailpitContainer(int smtpPort, int uiPort) {
            super(DockerImageName.parse('axllent/mailpit'), smtpPort, uiPort)
        }

        @Override
        String getHost() {
            'localhost'
        }

        @Override
        Integer getMappedPort(int originalPort) {
            if (originalPort == smtpPort) {
                return 12525
            }
            if (originalPort == uiPort) {
                return 18080
            }
            throw new IllegalArgumentException("Unexpected port: $originalPort")
        }
    }
}
