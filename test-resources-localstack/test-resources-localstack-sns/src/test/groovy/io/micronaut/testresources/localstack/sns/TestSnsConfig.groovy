package io.micronaut.testresources.localstack.sns

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("aws")
class TestSnsConfig {
    String accessKeyId
    String secretKey
    String region

    @ConfigurationBuilder(configurationPrefix = "services.sns")
    final Sns sns = new Sns()

    static class Sns {
        String endpointOverride
    }
}
