package io.micronaut.testresources.seaweedfs

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.DockerClientFactory
import spock.util.concurrent.PollingConditions
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration

@MicronautTest
class SeaweedFsStartedTest extends AbstractTestContainersSpec {

    @Value('${seaweedfs.url}')
    String url

    @Value('${seaweedfs.access-key}')
    String accessKey

    @Value('${seaweedfs.secret-key}')
    String secretKey

    @Override
    String getScopeName() {
        'seaweedfs'
    }

    def "automatically starts a SeaweedFS container"() {
        given:
        def dockerHost = DockerClientFactory.instance().dockerHostIpAddress()
        def bucketName = "test-bucket-${System.nanoTime()}"
        def conditions = new PollingConditions(timeout: 15, initialDelay: 1, delay: 1)

        when:
        conditions.eventually {
            buildClient().withCloseable { client ->
                client.listBuckets()
            }
        }

        String read
        buildClient().withCloseable { client ->
            client.createBucket {
                it.bucket(bucketName)
            }
            client.putObject({
                it.bucket(bucketName)
                it.key("test-key")
            }, RequestBody.fromString("test data"))
            read = client.getObject({
                it.bucket(bucketName)
                it.key("test-key")
            }, ResponseTransformer.toBytes()).asUtf8String()
        }

        then:
        listContainers().size() == 1
        url.contains(dockerHost)
        accessKey == SeaweedFsTestResourceProvider.DEFAULT_ACCESS_KEY
        secretKey == SeaweedFsTestResourceProvider.DEFAULT_SECRET_KEY
        read.readLines() == ["test data"]
        with(TestContainers.listByScope("seaweedfs").get(Scope.of("seaweedfs"))) {
            size() == 1
            get(0).dockerImageName.startsWith(SeaweedFsTestResourceProvider.DEFAULT_IMAGE)
        }
    }

    private S3Client buildClient() {
        S3Client.builder()
            .endpointOverride(new URI(url))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .region(Region.US_EAST_1)
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .build()
    }
}
