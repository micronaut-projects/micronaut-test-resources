package io.micronaut.testresources.seaweedfs

import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

class SeaweedFsTestResourceProviderTest extends Specification {
    private final provider = new ExposedSeaweedFsTestResourceProvider()

    def "exposes resolver metadata and supported properties"() {
        expect:
        provider.name == "containers.seaweedfs"
        provider.displayName == SeaweedFsTestResourceProvider.DISPLAY_NAME
        provider.exposedSimpleName() == SeaweedFsTestResourceProvider.SIMPLE_NAME
        provider.exposedDefaultImageName() == SeaweedFsTestResourceProvider.DEFAULT_IMAGE
        provider.getResolvableProperties([:], [:]) == [
                SeaweedFsTestResourceProvider.SEAWEEDFS_URL,
                SeaweedFsTestResourceProvider.SEAWEEDFS_ACCESS_KEY,
                SeaweedFsTestResourceProvider.SEAWEEDFS_SECRET_KEY
        ]
        provider.exposedShouldAnswer(SeaweedFsTestResourceProvider.SEAWEEDFS_URL)
        provider.exposedShouldAnswer(SeaweedFsTestResourceProvider.SEAWEEDFS_ACCESS_KEY)
        provider.exposedShouldAnswer(SeaweedFsTestResourceProvider.SEAWEEDFS_SECRET_KEY)
        !provider.exposedShouldAnswer("seaweedfs.bucket")
    }

    def "maps container values to supported seaweedfs properties"() {
        given:
        def container = Mock(GenericContainer) {
            getHost() >> "127.0.0.1"
            getMappedPort(SeaweedFsTestResourceProvider.S3_PORT) >> 8333
        }

        expect:
        provider.exposedResolveProperty(SeaweedFsTestResourceProvider.SEAWEEDFS_URL, container).get() == "http://127.0.0.1:8333"
        provider.exposedResolveProperty(SeaweedFsTestResourceProvider.SEAWEEDFS_ACCESS_KEY, container).get() == SeaweedFsTestResourceProvider.DEFAULT_ACCESS_KEY
        provider.exposedResolveProperty(SeaweedFsTestResourceProvider.SEAWEEDFS_SECRET_KEY, container).get() == SeaweedFsTestResourceProvider.DEFAULT_SECRET_KEY
        !provider.exposedResolveProperty("seaweedfs.bucket", container).present
    }

    private static final class ExposedSeaweedFsTestResourceProvider extends SeaweedFsTestResourceProvider {
        String exposedSimpleName() {
            getSimpleName()
        }

        String exposedDefaultImageName() {
            getDefaultImageName()
        }

        boolean exposedShouldAnswer(String propertyName) {
            shouldAnswer(propertyName, [:], [:])
        }

        Optional<String> exposedResolveProperty(String propertyName, GenericContainer<?> container) {
            resolveProperty(propertyName, container)
        }
    }
}
