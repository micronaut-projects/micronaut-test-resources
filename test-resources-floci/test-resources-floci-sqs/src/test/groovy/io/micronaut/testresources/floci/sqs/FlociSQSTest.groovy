package io.micronaut.testresources.floci.sqs

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.floci.AbstractFlociSpec
import jakarta.inject.Inject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@MicronautTest
class FlociSQSTest extends AbstractFlociSpec {

    @Inject
    SQSConfig sqsConfig

    def "exposes SQS endpoint override property"() {
        expect:
        new FlociSQSService().resolvableProperties == ["aws.services.sqs.endpoint-override"]
    }

    def "automatically starts an SQS container"() {
        given:
        def client = buildClient()

        when:
        client.listQueues()

        then:
        noExceptionThrown()

        and:
        listContainers().size() == 1
    }

    private SqsClient buildClient() {
        SqsClient.builder()
                .endpointOverride(new URI(sqsConfig.sqs.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(sqsConfig.accessKeyId, sqsConfig.secretKey)
                        )
                )
                .region(Region.of(sqsConfig.region))
                .build()
    }

    @ConfigurationProperties("aws")
    static class SQSConfig {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "services.sqs")
        final SQS sqs = new SQS()

        static class SQS {
            String endpointOverride
        }
    }
}
