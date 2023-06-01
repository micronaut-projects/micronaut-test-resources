package io.micronaut.testresources.localstack.sqs

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@MicronautTest
class LocalStackSQSTest extends AbstractLocalStackSpec {

    @Inject
    SqsConfig sqsConfig

    def "automatically starts an SQS container"() {
        given:
        def client = buildClient()

        when:
        def queue = client.createQueue {
            it.queueName("test-queue")
        }

        client.sendMessage {
            it.queueUrl(queue.queueUrl())
            it.messageBody("test-message-body")
        }

        then:
        def response = client.receiveMessage {
            it.queueUrl(queue.queueUrl())
            it.maxNumberOfMessages(1)
        }
        response.hasMessages()
        response.messages().size() == 1
        response.messages().get(0).body() == "test-message-body"

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
    static class SqsConfig {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "services.sqs")
        final Sqs sqs = new Sqs()

        static class Sqs {
            String endpointOverride
        }
    }
}
