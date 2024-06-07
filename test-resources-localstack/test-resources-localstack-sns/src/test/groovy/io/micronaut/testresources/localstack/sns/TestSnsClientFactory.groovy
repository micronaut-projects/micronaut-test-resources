package io.micronaut.testresources.localstack.sns

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Factory
class TestSnsClientFactory {

    @Singleton
    SnsClient snsClient(TestSnsConfig testSnsConfig) {
        return SnsClient.builder()
                .endpointOverride(new URI(testSnsConfig.sns.endpointOverride))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(testSnsConfig.accessKeyId, testSnsConfig.secretKey)
                        )
                )
                .region(Region.of(testSnsConfig.region))
                .build()
    }
}
