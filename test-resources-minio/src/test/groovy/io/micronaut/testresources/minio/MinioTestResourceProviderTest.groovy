package io.micronaut.testresources.minio

import org.testcontainers.containers.MinIOContainer
import spock.lang.Specification

class MinioTestResourceProviderTest extends Specification {
    private final provider = new ExposedMinioTestResourceProvider()

    def "exposes resolver metadata and supported properties"() {
        expect:
        provider.name == "containers.minio"
        provider.displayName == MinioTestResourceProvider.DISPLAY_NAME
        provider.exposedSimpleName() == MinioTestResourceProvider.SIMPLE_NAME
        provider.exposedDefaultImageName() == MinioTestResourceProvider.DEFAULT_IMAGE
        provider.getResolvableProperties([:], [:]) == [
                MinioTestResourceProvider.MINIO_URL,
                MinioTestResourceProvider.MINIO_ACCESS_KEY,
                MinioTestResourceProvider.MINIO_SECRET_KEY
        ]
        provider.exposedShouldAnswer(MinioTestResourceProvider.MINIO_URL)
        provider.exposedShouldAnswer(MinioTestResourceProvider.MINIO_ACCESS_KEY)
        provider.exposedShouldAnswer(MinioTestResourceProvider.MINIO_SECRET_KEY)
        !provider.exposedShouldAnswer("minio.bucket")
    }

    def "maps container values to supported minio properties"() {
        given:
        def container = Mock(MinIOContainer) {
            getS3URL() >> "http://127.0.0.1:9000"
            getUserName() >> "minio-user"
            getPassword() >> "minio-secret"
        }

        expect:
        provider.exposedResolveProperty(MinioTestResourceProvider.MINIO_URL, container).get() == "http://127.0.0.1:9000"
        provider.exposedResolveProperty(MinioTestResourceProvider.MINIO_ACCESS_KEY, container).get() == "minio-user"
        provider.exposedResolveProperty(MinioTestResourceProvider.MINIO_SECRET_KEY, container).get() == "minio-secret"
        !provider.exposedResolveProperty("minio.bucket", container).present
    }

    private static final class ExposedMinioTestResourceProvider extends MinioTestResourceProvider {
        String exposedSimpleName() {
            getSimpleName()
        }

        String exposedDefaultImageName() {
            getDefaultImageName()
        }

        boolean exposedShouldAnswer(String propertyName) {
            shouldAnswer(propertyName, [:], [:])
        }

        Optional<String> exposedResolveProperty(String propertyName, MinIOContainer container) {
            resolveProperty(propertyName, container)
        }
    }
}
