package io.micronaut.testresources.localstack.ses

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.Topic

@MicronautTest
class LocalStackSESTest extends AbstractLocalStackSpec {

    @Inject
    SesConfig sesConfig

    def "automatically starts a SES container"() {
        given:
        def client = buildSesClient()
        client.verifyEmailIdentity {
            it.emailAddress("sender@example.com")
        }

        when:
        def result = client.sendEmail {
            it.source("sender@example.com")
            it.destination {
                it.toAddresses("recipient@example.com")
            }
            it.message {
                it.subject {
                    it.data("Micronaut Test Resources")
                }
                it.body {
                    it.text {
                        it.data("SES message from LocalStack")
                    }
                }
            }
        }

        then:
        result.messageId()

        and:
        listContainers().size() == 1
    }

    def "uses a shared LocalStack container with SNS"() {
        given:
        def sesClient = buildSesClient()
        def snsClient = buildSnsClient()

        when:
        sesClient.verifyEmailIdentity {
            it.emailAddress("shared@example.com")
        }
        snsClient.createTopic {
            it.name("ses-shared-localstack")
        }

        then:
        sesClient.listIdentities().identities().contains("shared@example.com")
        List<Topic> topics = snsClient.listTopics().topics()
        topics.any { it.topicArn().endsWith("ses-shared-localstack") }

        and:
        listContainers().size() == 1
    }

    private SesClient buildSesClient() {
        SesClient.builder()
                .endpointOverride(new URI(sesConfig.ses.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(sesConfig.accessKeyId, sesConfig.secretKey)
                        )
                )
                .region(Region.of(sesConfig.region))
                .build()
    }

    private SnsClient buildSnsClient() {
        SnsClient.builder()
                .endpointOverride(new URI(sesConfig.sns.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(sesConfig.accessKeyId, sesConfig.secretKey)
                        )
                )
                .region(Region.of(sesConfig.region))
                .build()
    }

    @ConfigurationProperties("aws")
    static class SesConfig {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "services.ses")
        final Ses ses = new Ses()

        @ConfigurationBuilder(configurationPrefix = "services.sns")
        final Sns sns = new Sns()

        static class Ses {
            String endpointOverride
        }

        static class Sns {
            String endpointOverride
        }
    }
}
