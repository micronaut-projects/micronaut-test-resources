package io.micronaut.testresources.minio

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import org.testcontainers.DockerClientFactory

@MicronautTest
class MinioStartedTest extends AbstractTestContainersSpec {

    @Value('${minio.url}')
    String url

    @Value('${minio.access-key}')
    String accessKey

    @Value('${minio.access-key}')
    String secretKey

    @Override
    String getScopeName() {
        'minio'
    }

    def "automatically starts a MinIO container"() {
        given:
        def dockerHost = DockerClientFactory.instance().dockerHostIpAddress()

        expect:
        dockerHost in ["localhost", "127.0.0.1"]
        listContainers().size() == 1
        url.contains(dockerHost)
        accessKey == "minioadmin"
        secretKey == "minioadmin"
    }

}
