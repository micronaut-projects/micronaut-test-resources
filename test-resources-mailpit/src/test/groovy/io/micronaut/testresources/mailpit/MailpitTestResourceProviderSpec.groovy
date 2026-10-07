package io.micronaut.testresources.mailpit

import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.value.MapPropertyResolver
import io.micronaut.testresources.core.PropertyResolverSupport
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

class MailpitTestResourceProviderSpec extends Specification {

    private static final String SMTP_PREFIX = MailpitTestResourceProvider.JAVAMAIL_SMTP_PREFIX
    private static final String MAILPIT_PREFIX = MailpitTestResourceProvider.MAILPIT_PREFIX
    private static final List<String> ALL_PROPERTIES = [
            MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST,
            MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT,
            MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH,
            MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS,
            MailpitTestResourceProvider.MAILPIT_UI_URL,
            MailpitTestResourceProvider.MAILPIT_API_URL
    ]

    private final MailpitTestResourceProvider provider = new MailpitTestResourceProvider()

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

    def "resolves every Mailpit property when no SMTP endpoint is configured"() {
        expect:
        provider.getResolvableProperties(propertyEntries, [:]) == ALL_PROPERTIES

        where:
        propertyEntries << [
                [:],
                [(SMTP_PREFIX): []],
                [(SMTP_PREFIX): ['auth', 'starttls']]
        ]
    }

    def "keeps resolving its own offer once the test resources property source is loaded"() {
        given: 'the entries an environment reports after this provider offered its properties'
        def propertyEntries = [
                (SMTP_PREFIX)   : ['host', 'port', 'auth', 'starttls'],
                (MAILPIT_PREFIX): ['ui', 'api']
        ]

        expect: 'the endpoint entries are recognised as this provider own offer, not as user configuration'
        provider.getResolvableProperties(propertyEntries, [:]) == ALL_PROPERTIES
    }

    def "declines to resolve anything when an SMTP endpoint is already configured"() {
        expect:
        provider.getResolvableProperties([(SMTP_PREFIX): configuredEntries], [:]).isEmpty()

        where:
        configuredEntries << [
                ['host'],
                ['port'],
                ['host', 'port'],
                ['host', 'auth', 'starttls'],
                ['host', 'port', 'auth', 'starttls']
        ]
    }

    def "reads the JavaMail SMTP and Mailpit property entries"() {
        expect:
        provider.getRequiredPropertyEntries() == [SMTP_PREFIX, MAILPIT_PREFIX]
    }

    def "never requires a property that it resolves itself"() {
        given:
        def resolvable = provider.getResolvableProperties([:], [:])

        expect: 'otherwise resolving one of them asks for the other, forever'
        resolvable.collectMany { provider.getRequiredProperties(it) }.intersect(resolvable).isEmpty()
    }

    def "resolving #property does not recurse"() {
        given: 'a property resolver which answers each nested request on its own thread, like the test resources server does'
        def propertyResolver = new RecursingPropertyResolver(provider)

        when:
        PropertyResolverSupport.resolveRequiredProperties(property, propertyResolver, provider)

        then:
        noExceptionThrown()

        where:
        property << [
                MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_PORT,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_AUTH,
                MailpitTestResourceProvider.JAVAMAIL_SMTP_STARTTLS,
                MailpitTestResourceProvider.MAILPIT_UI_URL,
                MailpitTestResourceProvider.MAILPIT_API_URL
        ]
    }

    def "only supports Mailpit properties"() {
        expect:
        provider.shouldAnswer(MailpitTestResourceProvider.JAVAMAIL_SMTP_HOST, [:], [:])
        !provider.shouldAnswer('smtp.host', [:], [:])
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
        container.commandParts == ['--smtp', '[::]:2525', '--listen', '[::]:8080']
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

    /**
     * Models the loop which resolves a test resources property: every property the provider
     * declares resolvable is itself resolved by asking the provider again, and each of those
     * turns runs on a fresh thread, because the test resources server answers each client
     * request on a request thread. A provider which requires a property it also resolves
     * therefore loops until the stack overflows; this resolver fails fast instead.
     */
    private static class RecursingPropertyResolver extends MapPropertyResolver {

        private static final int MAX_DEPTH = 20

        private final MailpitTestResourceProvider provider
        private final List<String> resolvable
        private int depth

        RecursingPropertyResolver(MailpitTestResourceProvider provider) {
            super([:])
            this.provider = provider
            this.resolvable = provider.getResolvableProperties([:], [:])
        }

        @Override
        <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
            if (!resolvable.contains(name)) {
                return super.getProperty(name, conversionContext)
            }
            resolveOnNewThread(name)
            return ConversionService.SHARED.convert("value-of-$name".toString(), conversionContext)
        }

        private void resolveOnNewThread(String name) {
            depth++
            try {
                if (depth > MAX_DEPTH) {
                    throw new IllegalStateException("Resolution of '$name' recursed more than $MAX_DEPTH levels deep")
                }
                def failure = new AtomicReference<Throwable>()
                def thread = new Thread({
                    try {
                        PropertyResolverSupport.resolveRequiredProperties(name, this, provider)
                    } catch (Throwable t) {
                        failure.set(t)
                    }
                })
                thread.start()
                thread.join()
                if (failure.get() != null) {
                    throw failure.get()
                }
            } finally {
                depth--
            }
        }
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
