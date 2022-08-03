package io.micronaut.testresources.aws.localstack.s3

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.aws.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@MicronautTest
class LocalStackS3Test extends AbstractLocalStackSpec {

    @Inject
    S3Config s3Config

    def "automatically starts an S3 container"() {
        given:
        def client = buildClient()

        when:
        def bucket = client.createBucket {
            it.bucket("test-bucket")
        }
        client.putObject({
            it.bucket("test-bucket")
            it.key("test-key")
        }, RequestBody.fromString("test data"))

        then:
        def read = client.getObject {
            it.bucket("test-bucket")
            it.key("test-key")
        }
        read.readLines() == ["test data"]

        and:
        listContainers().size() == 1
    }

    private S3Client buildClient() {
        S3Client.builder()
                .endpointOverride(new URI(s3Config.s3.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(s3Config.accessKeyId, s3Config.secretKey)
                        )
                )
                .region(Region.of(s3Config.region))
                .build()
    }

    @ConfigurationProperties("aws")
    static class S3Config {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "s3")
        final S3 s3 = new S3()

        static class S3 {
            String endpointOverride
        }
    }
}
