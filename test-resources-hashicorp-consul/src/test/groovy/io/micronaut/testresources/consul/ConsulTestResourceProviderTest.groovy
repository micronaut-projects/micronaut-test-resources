package io.micronaut.testresources.consul

import org.testcontainers.consul.ConsulContainer
import spock.lang.Specification

class ConsulTestResourceProviderTest extends Specification {
    private final provider = new ExposedConsulTestResourceProvider()

    def "exposes resolver metadata and supported properties"() {
        expect:
        provider.displayName == ConsulTestResourceProvider.DISPLAY_NAME
        provider.exposedSimpleName() == ConsulTestResourceProvider.SIMPLE_NAME
        provider.exposedDefaultImageName() == ConsulTestResourceProvider.DEFAULT_IMAGE
        provider.getResolvableProperties([:], [:]) == ConsulTestResourceProvider.RESOLVABLE_PROPERTIES_LIST
        provider.exposedShouldAnswer(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_HOST)
        provider.exposedShouldAnswer(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_PORT)
        provider.exposedShouldAnswer(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_DEFAULT_ZONE)
        !provider.exposedShouldAnswer("consul.server.host")
    }

    def "maps container values to supported consul properties"() {
        given:
        def container = Mock(ConsulContainer) {
            getHost() >> "127.0.0.1"
            getMappedPort(ConsulTestResourceProvider.CONSUL_HTTP_PORT) >> 32768
        }

        expect:
        provider.exposedResolveProperty(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_HOST, container).get() == "127.0.0.1"
        provider.exposedResolveProperty(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_PORT, container).get() == "32768"
        provider.exposedResolveProperty(ConsulTestResourceProvider.PROPERTY_CONSUL_CLIENT_DEFAULT_ZONE, container).get() == "127.0.0.1:32768"
        !provider.exposedResolveProperty("consul.client.datacenter", container).present
        !provider.exposedResolveProperty("consul.server.host", container).present
    }

    private static final class ExposedConsulTestResourceProvider extends ConsulTestResourceProvider {
        String exposedSimpleName() {
            getSimpleName()
        }

        String exposedDefaultImageName() {
            getDefaultImageName()
        }

        boolean exposedShouldAnswer(String propertyName) {
            shouldAnswer(propertyName, [:], [:])
        }

        Optional<String> exposedResolveProperty(String propertyName, ConsulContainer container) {
            resolveProperty(propertyName, container)
        }
    }
}
